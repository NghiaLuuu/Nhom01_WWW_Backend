package vn.edu.iuh.nhom01_www.aivector;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

class AiVectorConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PgVectorStoreAutoConfiguration.class));

    @Test
    void defaultVectorStoreTypePreventsPgVectorAutoConfigurationWithoutDatabaseExtensions() {
        contextRunner
                .withPropertyValues("spring.ai.vectorstore.type=none")
                .run(context -> assertThat(context).doesNotHaveBean(PgVectorStore.class));
    }

    @Test
    void aiVectorProfileUsesCosineHnswAndOpenAiEmbeddingWithoutCallingOpenAi() {
        Properties properties = yamlProperties("application-ai-vector.yml");

        assertEquals("ai-vector", properties.getProperty("spring.config.activate.on-profile"));
        assertEquals("${OPENAI_API_KEY}", properties.getProperty("spring.ai.openai.api-key"));
        assertEquals("text-embedding-3-small", properties.getProperty("spring.ai.openai.embedding.model"));
        assertEquals("1536", properties.getProperty("spring.ai.openai.embedding.dimensions"));
        assertEquals("pgvector", properties.getProperty("spring.ai.vectorstore.type"));
        assertEquals("COSINE_DISTANCE", properties.getProperty("spring.ai.vectorstore.pgvector.distance-type"));
        assertEquals("HNSW", properties.getProperty("spring.ai.vectorstore.pgvector.index-type"));
        assertEquals("false", properties.getProperty("spring.ai.vectorstore.pgvector.initialize-schema"));
        assertEquals("false", properties.getProperty("spring.ai.vectorstore.pgvector.remove-existing-vector-store-table"));
    }

    @Test
    void exampleConfigurationKeepsPgVectorStoreDisabledByDefault() {
        Properties properties = yamlProperties("application.example.yml");

        assertEquals("none", properties.getProperty("spring.ai.vectorstore.type"));
    }

    private Properties yamlProperties(String resourceName) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        return factory.getObject();
    }
}
