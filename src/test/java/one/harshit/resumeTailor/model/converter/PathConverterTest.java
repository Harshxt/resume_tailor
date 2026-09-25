package one.harshit.resumeTailor.model.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class PathConverterTest {

    private PathConverter pathConverter;

    @BeforeEach
    void setUp() {
        pathConverter = new PathConverter();
    }

    @Test
    @DisplayName("Test 1: Convert Path to database column (valid path and null)")
    void convertToDatabaseColumn_ValidAndNull() {
        // Given a valid Path
        Path samplePath = Paths.get("files", "upload", "sample_resume.pdf");

        // When converting to database column
        String dbColumn = pathConverter.convertToDatabaseColumn(samplePath);

        // Then it should match the string representation
        assertThat(dbColumn).isEqualTo(samplePath.toString());

        // When converting null
        String nullDbColumn = pathConverter.convertToDatabaseColumn(null);

        // Then it should return null
        assertThat(nullDbColumn).isNull();
    }

    @Test
    @DisplayName("Test 2: Convert database column string to Path entity attribute (valid string and null)")
    void convertToEntityAttribute_ValidAndNull() {
        // Given a valid DB path string
        String rawPath = "files/upload/sample_resume.pdf";

        // When converting to entity attribute
        Path attributePath = pathConverter.convertToEntityAttribute(rawPath);

        // Then it should convert to a valid Path
        assertThat(attributePath).isNotNull();
        assertThat(attributePath).isEqualTo(Paths.get(rawPath));

        // When converting null
        Path nullAttribute = pathConverter.convertToEntityAttribute(null);

        // Then it should return null
        assertThat(nullAttribute).isNull();
    }
}