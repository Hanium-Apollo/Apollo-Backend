package com.Teletubbies.Apollo.board.dto.post.response;

import lombok.Data;

@Data
public class UpdatePostResponse {
    private Long postId;
    public UpdatePostResponse(Long postId) {
        this.postId = postId;
    }
}
