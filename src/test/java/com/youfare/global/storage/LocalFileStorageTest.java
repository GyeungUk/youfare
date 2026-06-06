package com.youfare.global.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로컬 스토리지 저장/삭제 단위 테스트.
 * 자격증명 없이 디스크 입출력만으로 저장 → URL 생성 → 삭제 흐름을 검증한다.
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage newStorage() {
        StorageProperties.Local props = new StorageProperties.Local();
        props.setBaseDir(tempDir.toString());
        props.setBaseUrl("http://localhost:8080/uploads");
        return new LocalFileStorage(props);
    }

    @Test
    @DisplayName("저장하면 파일이 디스크에 기록되고, baseUrl이 붙은 접근 URL을 돌려준다")
    void storeWritesFileAndReturnsUrl() {
        LocalFileStorage storage = newStorage();

        StoredFile stored = storage.store(new byte[]{1, 2, 3}, "image/png", "png");

        assertThat(stored.key()).startsWith("posts/").endsWith(".png");
        assertThat(stored.url()).isEqualTo("http://localhost:8080/uploads/" + stored.key());
        assertThat(Files.exists(tempDir.resolve(stored.key()))).isTrue();
    }

    @Test
    @DisplayName("삭제하면 저장된 파일이 사라진다")
    void deleteRemovesFile() {
        LocalFileStorage storage = newStorage();
        StoredFile stored = storage.store(new byte[]{1, 2, 3}, "image/png", "png");
        Path saved = tempDir.resolve(stored.key());
        assertThat(Files.exists(saved)).isTrue();

        storage.delete(stored.key());

        assertThat(Files.exists(saved)).isFalse();
    }

    @Test
    @DisplayName("baseUrl 끝의 슬래시는 정규화되어 URL이 중복 슬래시 없이 생성된다")
    void normalizesTrailingSlash() {
        StorageProperties.Local props = new StorageProperties.Local();
        props.setBaseDir(tempDir.toString());
        props.setBaseUrl("http://localhost:8080/uploads/");   // 끝 슬래시
        LocalFileStorage storage = new LocalFileStorage(props);

        StoredFile stored = storage.store(new byte[]{9}, "image/gif", "gif");

        assertThat(stored.url()).isEqualTo("http://localhost:8080/uploads/" + stored.key());
        assertThat(stored.url()).doesNotContain("//uploads");
    }
}
