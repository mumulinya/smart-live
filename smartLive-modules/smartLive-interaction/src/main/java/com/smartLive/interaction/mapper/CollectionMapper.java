package com.smartLive.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;;
import com.smartLive.interaction.domain.Collection;


import java.util.List;

/**
 * 关注Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface CollectionMapper extends BaseMapper<Collection>
{
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    public Collection selectCollectionShopById(Long id);

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注集合
     */
    public List<Collection> selectCollectionShopList(Collection follow);

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int insertCollectionShop(Collection follow);

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    public int updateCollectionShop(Collection follow);

    /**
     * 删除关注
     * 
     * @param id 关注主键
     * @return 结果
     */
    public int deleteCollectionShopById(Long id);

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCollectionShopByIds(Long[] ids);
}
