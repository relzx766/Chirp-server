package com.zyq.chirp.mediaserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zyq.chirp.common.domain.enums.ContentType;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.exception.FileExistsException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.util.StringUtil;
import com.zyq.chirp.mediaclient.dto.ChunkUploadReqDto;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import com.zyq.chirp.mediaserver.convertor.MediaConvertor;
import com.zyq.chirp.mediaserver.domain.entity.CustomMinioClient;
import com.zyq.chirp.mediaserver.domain.pojo.Media;
import com.zyq.chirp.mediaserver.domain.properties.MinioProperties;
import com.zyq.chirp.mediaserver.mapper.MediaMapper;
import com.zyq.chirp.mediaserver.service.MediaService;
import com.zyq.chirp.mediaserver.util.FileUtil;
import com.zyq.chirp.mediaserver.util.MinioUtil;
import io.minio.ListPartsResponse;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Owner;
import io.minio.messages.Part;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.openxml4j.opc.ContentTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "upload:finish:hash")
@Slf4j
public class MediaServiceImpl implements MediaService {
    private static final String BASE_DIR = "D:\\Project\\static\\media\\";
    @Resource
    MediaMapper mediaMapper;
    @Resource
    MediaConvertor mediaConvertor;
    @Value("${default-config.file.upload.url}")
    String UPLOAD_SITE;
    @Resource
    RedisTemplate redisTemplate;
    static final String CHUNK_UPLOAD_CACHE = "upload:chunk:urls";
    static final String CHUNK_HASH_CACHE = "upload:chunk:hash";
    //@Resource
    CustomMinioClient minioClient;
    // @Resource
    MinioProperties minioProperties;
    //@Resource
    MinioUtil minioUtil;

    @Override
    public MediaDto getById(Integer id) {
        return mediaConvertor.pojoToDto(
                mediaMapper.selectOne(new LambdaQueryWrapper<Media>().eq(Media::getId, id)));
    }

    @Override
    public List<MediaDto> getById(List<Integer> id) {
        return mediaMapper.selectList(new LambdaQueryWrapper<Media>().in(Media::getId, id))
                .stream()
                .map(media -> mediaConvertor.pojoToDto(media)).toList();
    }

    @Override
    public Map<Long, List<MediaDto>> getByMap(Map<Long, List<Integer>> map) {
        return map.entrySet().stream()
                .map(entry -> {
                    List<Integer> ids = entry.getValue();
                    return ids.isEmpty() ? Map.entry(entry.getKey(), List.<MediaDto>of()) : Map.entry(entry.getKey(), getById(ids));

                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<MediaDto> getUrlById(List<Integer> id) {
        return mediaMapper.selectList(new LambdaQueryWrapper<Media>().select(Media::getUrl, Media::getType, Media::getSize, Media::getId).in(Media::getId, id))
                .stream()
                .map(media -> mediaConvertor.pojoToDto(media)).toList();
    }

    @Override
    public Map<Long, List<MediaDto>> getUrlByMap(Map<Long, List<Integer>> map) {
        return map.entrySet().stream()
                .map(entry -> {
                    List<Integer> ids = entry.getValue();
                    return ids.isEmpty() ? Map.entry(entry.getKey(), List.<MediaDto>of()) : Map.entry(entry.getKey(), getUrlById(ids));

                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Cacheable(key = "#md5")
    public MediaDto getByMd5(String md5) {
        String key = STR."upload:finish:hash:\{md5}";
        ValueOperations<String, MediaDto> operations = redisTemplate.opsForValue();
        MediaDto mediaDto = operations.get(key);
        if (mediaDto != null) {
            return mediaDto;
        }
        return mediaConvertor.pojoToDto(
                mediaMapper.selectOne(new LambdaQueryWrapper<Media>().eq(Media::getMd5, md5)));
    }


    @Override
    public List<MediaDto> getByIdList(List<Integer> ids) {
        return mediaMapper.selectList(new LambdaQueryWrapper<Media>().eq(Media::getId, ids))
                .stream()
                .map(media -> mediaConvertor.pojoToDto(media)).toList();
    }

    @Override
    public MediaDto save(MediaDto mediaDto) {
        Media media = mediaConvertor.dtoToPojo(mediaDto);
        mediaMapper.insert(media);
        return mediaConvertor.pojoToDto(media);
    }

    @Override
    public MediaDto saveFile(MultipartFile file) {
        try {
            String hex = DigestUtils.md5Hex(file.getBytes());
            MediaDto byMd5 = getByMd5(hex);
            if (byMd5 != null) {
                throw new FileExistsException(byMd5);
            }
            String filePath = STR."\{LocalDate.now()}\\\{hex}.\{FileUtil.getExtension(file.getOriginalFilename())}";
            Path path = Paths.get(BASE_DIR + filePath);
            FileUtil.creatNewFile(path);
            Files.write(path, file.getBytes());
            MediaDto mediaDto = MediaDto.builder()
                    .md5(hex)
                    .size(file.getSize())
                    .extension(FileUtil.getExtension(file.getOriginalFilename()))
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .url(UPLOAD_SITE + filePath)
                    .build();
            return save(mediaDto);
        } catch (IOException e) {
            log.error("上传文件失败,错误=>", e);
            throw new ChirpException(Code.ERR_BUSINESS, "文件上传失败");
        }
    }

    @Override
    public ChunkUploadReqDto initChunkUpload(int chunkSize) {
        try {
          /*  ValueOperations<String, List<String>> operations = redisTemplate.opsForValue();
            String objectName = IdWorker.getIdStr();
            Map<String, String> reqParams = new HashMap<>();
            List<String> urls = new ArrayList<>();
            String uploadId = minioUtil.initMultiPartUpload(objectName);
            reqParams.put(CustomMinioClient.UPLOAD_ID, uploadId);
            for (int i = 0; i < chunkSize; i++) {
                reqParams.put(CustomMinioClient.PART_NUMBER, String.valueOf(i));
                String uploadUrl = minioUtil.getUploadUrl(minioProperties.getBucket(), objectName, reqParams);
                urls.add(uploadUrl);
            }
            operations.set(STR."\{CHUNK_UPLOAD_CACHE}:\{uploadId}", urls);*/
            String uploadId = IdWorker.getIdStr();
            return ChunkUploadReqDto.builder().uploadId(uploadId).objectName(uploadId).build();
        } catch (Exception e) {
            log.error("初始化文件分片上传失败,错误=>", e);
            throw new ChirpException(Code.ERR_BUSINESS, "初始化文件分片上传失败");
        }
    }

    @Override
    public void uploadChunk(String uploadId, int index, MultipartFile file) {
        try {
            String hash = DigestUtils.md5Hex(file.getBytes());
            Path path = Paths.get(BASE_DIR, uploadId, hash + "." + index);
            FileUtil.creatNewFile(path);
            Files.write(path, file.getBytes());
            /*ValueOperations<String, List<String>> operations = redisTemplate.opsForValue();
            List<String> urls = operations.get(STR."\{CHUNK_UPLOAD_CACHE}:\{uploadId}");
            chunkHashCache(uploadId, index, file);
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(file.getBytes(), MediaType.parse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE));
            Request request = new Request.Builder().url(urls.get(index)).put(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new ChirpException(Code.ERR_BUSINESS, "服务器异常，该切片上传失败");
                }
            }*/

        } catch (IOException e) {
            throw new ChirpException(Code.ERR_SYSTEM, "服务器异常，该切片上传失败");
        } catch (NullPointerException e) {
            throw new ChirpException(Code.ERR_BUSINESS, "切片索引异常");
        }
    }

    @Override
    public String chunkHashCache(String uploadId, int index, MultipartFile file) {
        try {
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            String hex = DigestUtils.md5Hex(file.getBytes());
            operations.set(STR."\{CHUNK_HASH_CACHE}:\{uploadId}:\{index}", hex);
            return hex;
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_BUSINESS, "获取文件内容失败");
        }
    }

/*    @Override
    public MediaDto mergeFile(ChunkUploadReqDto reqDto) {
        try {

            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            ListPartsResponse listPartsResponse = minioClient.listMultipart(
                    minioProperties.getBucket(),
                    minioProperties.getRegion(),
                    reqDto.getObjectName(),
                    reqDto.getChunkSize() + 10,
                    0,
                    reqDto.getUploadId(),
                    null, null);
            Set<String> keys = redisTemplate.keys(STR."\{CHUNK_HASH_CACHE}:\{reqDto.getUploadId()}:*");
            List<String> hashStrList = operations.multiGet(keys);
            redisTemplate.delete(keys);
            redisTemplate.delete(STR."\{CHUNK_UPLOAD_CACHE}:\{reqDto.getUploadId()}");
            String uniqueKey = StringUtil.combineKey(hashStrList);
            uniqueKey = DigestUtils.md5DigestAsHex(uniqueKey.getBytes(StandardCharsets.UTF_8));
            String objectName = STR."\{FileUtil.generateDirname()}/\{uniqueKey}.\{reqDto.getExtension()}";
            Multimap<String, String> headers = HashMultimap.create();
            headers.put("Content-Type", reqDto.getType());
            //这个合并一直会报The specified multipart upload does not exist. The upload ID may be invalid, or the upload may have been aborted or completed.
            //明明提供的uploadId是正确的未完成的，minioClient.listMultipart都能查询到完整的分片信息
            minioClient.completeMultipartUpload(
                    minioProperties.getBucket(),
                    minioProperties.getRegion(),
                    objectName,
                    reqDto.getUploadId(),
                    listPartsResponse.result().partList().toArray(new Part[]{}),
                    headers,
                    null);
            long size = minioClient.statObject(StatObjectArgs.builder().bucket(minioProperties.getBucket()).object(objectName).build()).size();
            String url = STR."\{minioProperties.getEndpoint()}/\{minioProperties.getBucket()}/\{objectName}";
            MediaDto mediaDto = MediaDto.builder()
                    .md5(uniqueKey)
                    .size(size)
                    .extension(reqDto.getExtension())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .url(url)
                    .build();
            return save(mediaDto);
        } catch (Exception e) {
            log.error("合并分片失败", e);
            throw new ChirpException(Code.ERR_SYSTEM, "文件上传失败");
        }
    }*/

    @Override
    public MediaDto saveFile(byte[] file, MediaDto mediaDto) {

        try {
            String filePath = STR."\{LocalDate.now()}\\\{mediaDto.getMd5()}.\{mediaDto.getExtension()}";
            Path path = Paths.get(BASE_DIR + filePath);
            FileUtil.creatNewFile(path);
            Files.write(path, file);
            mediaDto.setSize((long) file.length);
            mediaDto.setExtension(FileUtil.getExtension(filePath));
            mediaDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
            mediaDto.setUrl(UPLOAD_SITE + filePath);
            return save(mediaDto);
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_BUSINESS, e);
        }
    }

    @Override
    public void saveSlice(byte[] file, String hash, String filename, Integer seq) {
        MediaDto media = getByMd5(hash);
        if (media != null) {
            throw new FileExistsException(media);
        }
        try {
            String filePath = STR."\{BASE_DIR}\{hash}\\\{filename}.\{seq.toString()}";
            Path path = Paths.get(filePath);
            FileUtil.creatNewFile(path);
            Files.write(path, file);
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_SYSTEM, e.getMessage() + ";\n文件上传失败");
        }
    }

    @Override
    @Transactional
    public MediaDto mergeFile(ChunkUploadReqDto reqDto) {
        try {
            File dir = new File(BASE_DIR + reqDto.getUploadId());
            if (!dir.exists() || dir.listFiles() == null) {
                throw new ChirpException(Code.ERR_BUSINESS, "文件不存在，合并失败");
            }
            File[] files = dir.listFiles();
            Arrays.sort(files, (f1, f2) -> {
                String suffix1 = FileUtil.getExtension(f1.getName());
                String suffix2 = FileUtil.getExtension(f2.getName());
                int num1 = Integer.parseInt(suffix1);
                int num2 = Integer.parseInt(suffix2);
                return num1 - num2;
            });
            String dateDir = LocalDate.now().toString();
            String filePath = STR."\{dateDir}\\\{reqDto.getUploadId()}.\{reqDto.getExtension()}";
            Path path = Paths.get(BASE_DIR + filePath);
            FileUtil.creatNewFile(path);
            MediaDto mediaDto = new MediaDto();
            String[] hashList = new String[files.length];
            try (FileChannel in = FileChannel.open(path, StandardOpenOption.WRITE)) {
                long position = 0;
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    hashList[i] = FileUtil.getName(file);
                    try (FileChannel out = FileChannel.open(file.toPath())) {
                        in.transferFrom(out, position, position + out.size());
                        position += out.size();
                    }
                    Files.delete(file.toPath());
                }
                mediaDto.setSize(in.size());
            }
            mediaDto.setMd5(DigestUtils.md5Hex(StringUtil.combineKey(hashList)));
            mediaDto.setExtension(reqDto.getExtension());
            mediaDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
            mediaDto.setUrl(UPLOAD_SITE + filePath);
            return save(mediaDto);
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_BUSINESS, e);
        }
    }
}
