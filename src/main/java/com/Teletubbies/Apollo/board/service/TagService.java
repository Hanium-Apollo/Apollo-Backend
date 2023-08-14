package com.Teletubbies.Apollo.board.service;

import com.Teletubbies.Apollo.board.domain.Post;
import com.Teletubbies.Apollo.board.domain.PostWithTag;
import com.Teletubbies.Apollo.board.domain.Tag;
import com.Teletubbies.Apollo.board.repository.PostWithTagRepository;
import com.Teletubbies.Apollo.board.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    public List<Tag> saveTag(List<String> tagNames){
        List<Tag> tags = tagNames.stream()
                .map(tagName -> {
                    if (!tagRepository.existsByName(tagName)) {
                        return tagRepository.save(new Tag(tagName));
                    } else {
                        return tagRepository.findByName(tagName)
                                .orElseThrow(() -> new IllegalArgumentException("해당 태그에 대한 정보가 없습니다."));
                    }
                })
                .collect(Collectors.toList());
        return tags;
    }
}