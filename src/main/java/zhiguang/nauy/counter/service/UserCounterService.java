package zhiguang.nauy.counter.service;

/**
 * 用户维度计数服务接口。
 * 
 * <p>定义用户维度的计数操作（关注数、粉丝数、发帖数、获赞数等）。</p>
 */
public interface UserCounterService {
    
    /**
     * 增加关注数。
     */
    void incrementFollowings(long userId);
    
    /**
     * 减少关注数。
     */
    void decrementFollowings(long userId);
    
    /**
     * 增加粉丝数。
     */
    void incrementFollowers(long userId);
    
    /**
     * 减少粉丝数。
     */
    void decrementFollowers(long userId);
    
    /**
     * 增加发帖数。
     */
    void incrementPosts(long userId);
    
    /**
     * 增加获得的赞数。
     */
    void incrementLikesReceived(long userId);
    
    /**
     * 增加获得的收藏数。
     */
    void incrementFavsReceived(long userId);
    
    /**
     * 重建用户所有计数（从数据库回源）。
     */
    void rebuildAllCounters(long userId);
}
