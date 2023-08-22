package com.Teletubbies.Apollo.board.dto.post.response;

import com.Teletubbies.Apollo.board.dto.tag.ConvertTag;
import lombok.Data;

import java.util.List;
@Data
public class StartBoard {
    List<FindPostResponse> posts;
    List<ConvertTag> tags;

    public StartBoard(List<FindPostResponse> posts, List<ConvertTag> tags) {
        this.posts = posts;
        this.tags = tags;
    }
}