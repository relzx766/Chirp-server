import com.zyq.chirp.common.redis.util.BloomUtil;
import com.zyq.chirp.userserver.UserServerApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = UserServerApplication.class)
public class testa {
    @Resource
    BloomUtil bloomUtil;

    @Test
    public void tes() {
        System.out.println(bloomUtil.mExists("bloom:username", "1111", "aaaa", "genshit", "DonaldTrump"));
        System.out.println(bloomUtil.mExists("bloom:email", "aaaaaa@q.com", "aaaaaa@qq.com", "genshin@mihoyo.com", "DonaldTrump@white.house"));
        ;
    }
}
