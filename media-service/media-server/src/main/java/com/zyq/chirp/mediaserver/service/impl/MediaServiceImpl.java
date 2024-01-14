package com.zyq.chirp.mediaserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.exception.FileExistsException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import com.zyq.chirp.mediaserver.convertor.MediaConvertor;
import com.zyq.chirp.mediaserver.domain.pojo.Media;
import com.zyq.chirp.mediaserver.mapper.MediaMapper;
import com.zyq.chirp.mediaserver.service.MediaService;
import com.zyq.chirp.mediaserver.util.FileUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "upload:finish:hash")
public class MediaServiceImpl implements MediaService {
    private static final String BASE_DIR = "D:\\Project\\static\\media\\";
    @Resource
    MediaMapper mediaMapper;
    @Resource
    MediaConvertor mediaConvertor;
    @Value("${default-config.file.upload.url}")
    String UPLOAD_SITE;

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
    public MediaDto getUrlById(Integer id) {
        return mediaConvertor.pojoToDto(mediaMapper.selectOne(
                new LambdaQueryWrapper<Media>().select(Media::getUrl, Media::getType).eq(Media::getId, id)));
    }

    @Override
    public List<MediaDto> getUrlById(List<Integer> id) {
        return mediaMapper.selectList(new LambdaQueryWrapper<Media>().select(Media::getUrl, Media::getType).in(Media::getId, id))
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
    public MediaDto saveFile(byte[] file, MediaDto mediaDto) {
        MediaDto media = getByMd5(mediaDto.getMd5());
        if (media != null) {
            throw new FileExistsException(media);
        }
        try {
            String filePath = LocalDate.now() + "\\" + mediaDto.getMd5() + "." + mediaDto.getExtension();
            Path path = Paths.get(BASE_DIR + filePath);
            FileUtil.creatNewFile(path);
            Files.write(path, file);
            mediaDto.setName(path.getFileName().toString());
            mediaDto.setSize((long) file.length);
            mediaDto.setType(FileUtil.getMime(path));
            mediaDto.setCategory(FileUtil.getMediaCategory(mediaDto.getType()));
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
            String filePath = BASE_DIR + hash + "\\" + filename + "." + seq.toString();
            Path path = Paths.get(filePath);
            FileUtil.creatNewFile(path);
            Files.write(path, file);
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_SYSTEM, e.getMessage() + ";\n文件上传失败");
        }
    }

    @Override
    @Transactional
    public MediaDto mergeFile(MediaDto mediaDto) {
        MediaDto media = getByMd5(mediaDto.getMd5());
        if (media != null) {
            throw new FileExistsException(media);
        }
        try {
            File dir = new File(BASE_DIR + mediaDto.getMd5());
            if (!dir.exists() || dir.listFiles() == null) {
                throw new ChirpException(Code.ERR_BUSINESS, "文件不存在，合并失败");
            }
            File[] files = dir.listFiles();
            Arrays.sort(files, (f1, f2) -> {
                String suffix1 = FileUtil.getExtension(f1.getName());
                String suffix2 = FileUtil.getExtension(f1.getName());
                int num1 = Integer.parseInt(suffix1);
                int num2 = Integer.parseInt(suffix2);
                return num1 - num2;
            });
            String dateDir = LocalDate.now().toString();
            String filePath = dateDir + "\\" + mediaDto.getMd5() + "." + mediaDto.getExtension();
            Path path = Paths.get(BASE_DIR + filePath);
            FileUtil.creatNewFile(path);
            try (FileChannel in = FileChannel.open(path, StandardOpenOption.WRITE)) {
                long position = 0;
                for (File file : files) {
                    try (FileChannel out = FileChannel.open(file.toPath())) {
                        in.transferFrom(out, position, position + out.size());
                        position += out.size();
                        Files.delete(file.toPath());
                    }
                }
                mediaDto.setSize(in.size());
            }
            Files.delete(dir.toPath());
            mediaDto.setType(FileUtil.getMime(path));
            mediaDto.setCategory(FileUtil.getMediaCategory(mediaDto.getType()));
            mediaDto.setName(path.getFileName().toString());
            mediaDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
            mediaDto.setUrl(UPLOAD_SITE + filePath);
            return save(mediaDto);
        } catch (IOException e) {
            throw new ChirpException(Code.ERR_BUSINESS, e);
        }
    }
}
