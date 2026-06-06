package com.youfare.domain.community.media;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.response.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 게시글 첨부물(이미지·동영상·문서) 업로드 정책 검증기.
 *
 * <p>정책
 * <ul>
 *   <li>최대 <b>10개</b></li>
 *   <li>1개당 최대 <b>20MB</b></li>
 *   <li>총합 최대 <b>50MB</b></li>
 *   <li>이미지(jpg·png·gif·webp)·동영상(mp4·webm): <b>매직넘버 실검증</b>(위조 차단).
 *       브라우저가 직접 인라인 렌더링하므로 내용 위조 위험이 커서 시그니처를 확인한다.</li>
 *   <li>문서(pdf·doc·docx·xls·xlsx·ppt·pptx·hwp·hwpx·txt·csv·zip): 다운로드 전용이라
 *       확장자 화이트리스트로 허용. 화이트리스트 밖(실행 파일 등)은 차단.</li>
 * </ul>
 * 첨부는 선택 사항이며, 검증을 통과한 바이트만 반환하므로 저장소는 정책을 신경 쓰지 않아도 된다.
 */
@Component
public class MediaUploadValidator {

    private static final int MAX_COUNT = 10;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;   // 20MB
    private static final long MAX_TOTAL_SIZE = 50L * 1024 * 1024;  // 50MB

    /** 다운로드 전용 문서 확장자 → MIME. 매직넘버로 못 거르는 형식이라 확장자 화이트리스트로 허용한다. */
    private static final Map<String, String> FILE_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("hwp", "application/x-hwp"),
            Map.entry("hwpx", "application/hwp+zip"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("zip", "application/zip"));

    /**
     * @param files 업로드된 첨부물(없으면 null/빈 리스트 가능 — 첨부는 선택)
     * @return 검증 통과한 첨부물. 없으면 빈 리스트.
     */
    public List<ValidatedMedia> validate(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        // 빈 파트(브라우저가 빈 file input을 보내는 경우)는 무시한다.
        List<MultipartFile> nonEmpty = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();
        if (nonEmpty.isEmpty()) {
            return List.of();
        }
        if (nonEmpty.size() > MAX_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD);
        }

        List<ValidatedMedia> result = new ArrayList<>(nonEmpty.size());
        long total = 0;
        for (MultipartFile file : nonEmpty) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD);
            }
            byte[] bytes = readBytes(file);
            total += bytes.length;
            if (total > MAX_TOTAL_SIZE) {
                throw new BusinessException(ErrorCode.INVALID_MEDIA_UPLOAD);
            }

            String name = sanitizeName(file.getOriginalFilename());

            // 1) 이미지·동영상은 매직넘버로 실검증 — 통과 시 서버가 판별한 타입/확장자를 신뢰한다.
            var media = MediaType.detect(bytes);
            if (media.isPresent()) {
                MediaType t = media.get();
                result.add(new ValidatedMedia(bytes, t.getMimeType(), t.getExtension(), name, t.getKind()));
                continue;
            }

            // 2) 문서는 확장자 화이트리스트로 허용(다운로드 전용). 밖이면 차단.
            String ext = extensionOf(name);
            String fileMime = FILE_TYPES.get(ext);
            if (fileMime != null) {
                result.add(new ValidatedMedia(bytes, fileMime, ext, name, MediaKind.FILE));
                continue;
            }

            throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        return result;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** 경로/제어문자 제거 + 길이 제한으로 원본 파일명을 안전하게 정리한다. */
    private static String sanitizeName(String original) {
        if (original == null || original.isBlank()) {
            return "첨부파일";
        }
        // 경로 구분자 이후만 취해 디렉터리 정보를 버린다.
        String base = original.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[\\p{Cntrl}]", "").trim();
        if (base.isEmpty()) {
            return "첨부파일";
        }
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    /** 파일명에서 소문자 확장자만 추출(없으면 빈 문자열). */
    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1)
                ? name.substring(dot + 1).toLowerCase()
                : "";
    }

    /** 검증을 통과한 첨부물 하나. */
    public record ValidatedMedia(byte[] bytes, String contentType, String extension,
                                 String originalName, MediaKind kind) {
    }
}
