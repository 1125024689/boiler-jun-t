package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.boilerpojo.PostEntity;

@Mapper
public interface PostMapper {
    PostEntity getByPostId(String postId);

    int updateStatus(String postId, String status);
}
