package zhiguang.nauy.knowpost.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import zhiguang.nauy.knowpost.service.KnowPostsService;
import zhiguang.nauy.knowpost.mapper.KnowPostsMapper;
import org.springframework.stereotype.Service;

/**
* @author yuan
* @description 针对表【know_posts(知文主表-存储文章/帖子的核心元数据)】的数据库操作Service实现
* @createDate 2026-04-17 15:01:16
*/
@Service
public class KnowPostsServiceImpl extends ServiceImpl<KnowPostsMapper, KnowPosts>
    implements KnowPostsService {

}




