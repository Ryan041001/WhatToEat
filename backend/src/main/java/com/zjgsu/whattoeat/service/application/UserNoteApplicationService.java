package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.model.entity.UserRestaurantNoteEntity;
import com.zjgsu.whattoeat.repository.UserRepository;
import com.zjgsu.whattoeat.repository.UserRestaurantNoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserNoteApplicationService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final UserRepository userRepository;
    private final UserRestaurantNoteRepository userRestaurantNoteRepository;

    public UserNoteApplicationService(
            UserRepository userRepository,
            UserRestaurantNoteRepository userRestaurantNoteRepository) {
        this.userRepository = userRepository;
        this.userRestaurantNoteRepository = userRestaurantNoteRepository;
    }

    @Transactional
    public void createNote(Long userId, String poiId, String content) {
        validateUserExists(userId);
        validateContent(content);
        if (userRestaurantNoteRepository.existsByUserIdAndPoiId(userId, poiId)) {
            throw new BusinessException(ErrorCode.NOTE_ALREADY_EXISTS);
        }

        UserRestaurantNoteEntity entity = new UserRestaurantNoteEntity();
        entity.setUserId(userId);
        entity.setPoiId(poiId);
        entity.setNote(content.trim());
        try {
            userRestaurantNoteRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (!userRepository.existsById(userId)) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (userRestaurantNoteRepository.existsByUserIdAndPoiId(userId, poiId)) {
                throw new BusinessException(ErrorCode.NOTE_ALREADY_EXISTS);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public NotePage listNotes(Long userId, int page, int size, String keyword) {
        validateUserExists(userId);
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        Page<UserRestaurantNoteEntity> result;
        if (normalizedKeyword == null || normalizedKeyword.isEmpty()) {
            result = userRestaurantNoteRepository.findByUserIdOrderByUpdatedAtDescIdDesc(userId, pageRequest);
        } else {
            result = userRestaurantNoteRepository.findByUserIdAndNoteContainingOrderByUpdatedAtDescIdDesc(
                    userId,
                    normalizedKeyword,
                    pageRequest);
        }
        List<NoteItem> items = result.getContent().stream()
                .map(this::toNoteItem)
                .toList();
        return new NotePage(items, page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public NoteDetail getNote(Long userId, Long noteId) {
        validateUserExists(userId);
        return toNoteDetail(findNote(userId, noteId));
    }

    @Transactional
    public NoteDetail updateNote(Long userId, Long noteId, String content) {
        validateUserExists(userId);
        validateContent(content);
        UserRestaurantNoteEntity entity = findNote(userId, noteId);
        entity.setNote(content.trim());
        UserRestaurantNoteEntity saved = userRestaurantNoteRepository.saveAndFlush(entity);
        return toNoteDetail(saved);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        validateUserExists(userId);
        UserRestaurantNoteEntity entity = findNote(userId, noteId);
        userRestaurantNoteRepository.delete(entity);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.NOTE_CONTENT_INVALID);
        }
    }

    private UserRestaurantNoteEntity findNote(Long userId, Long noteId) {
        return userRestaurantNoteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
    }

    private NoteItem toNoteItem(UserRestaurantNoteEntity entity) {
        return new NoteItem(
                entity.getId(),
                entity.getPoiId(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private NoteDetail toNoteDetail(UserRestaurantNoteEntity entity) {
        return new NoteDetail(
                entity.getId(),
                entity.getUserId(),
                entity.getPoiId(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public record NotePage(List<NoteItem> items, int page, int size, long total) {
    }

    public record NoteItem(Long id, String poiId, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record NoteDetail(Long id, Long userId, String poiId, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
