package com.photoday.photoday.tag.service.impl;

import com.photoday.photoday.dto.MultiResponseDto;
import com.photoday.photoday.image.dto.ImageDto;
import com.photoday.photoday.image.entity.Image;
import com.photoday.photoday.image.mapper.ImageMapper;
import com.photoday.photoday.image.repository.ImageRepository;
import com.photoday.photoday.tag.entity.Tag;
import com.photoday.photoday.tag.repository.TagRepository;
import com.photoday.photoday.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final ImageMapper imageMapper;

    @Override
    public MultiResponseDto searchByTags(String tags, Pageable pageable) {
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber() - 1, pageable.getPageSize(), pageable.getSort());

        String[] tagArr = tags.split(",|\\s");
        List<String> tagList = Arrays.stream(tagArr).collect(Collectors.toList());

        Page<Image> page = imageRepository.findAllByTag(tagList, pageRequest);
        List<ImageDto.PageResponse> responses = page.stream()
                .map(imageMapper::imageToPageResponse).collect(Collectors.toList());

        return new MultiResponseDto(responses, page);
    }

    @Override
    public Tag verifyTag(Tag tag) {
        Optional<Tag> optionalTag = tagRepository.findByName(tag.getName());
        return optionalTag.orElseGet(() -> tagRepository.save(tag));
    }
}
