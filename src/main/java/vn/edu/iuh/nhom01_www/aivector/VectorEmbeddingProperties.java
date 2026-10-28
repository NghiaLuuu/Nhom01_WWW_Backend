package vn.edu.iuh.nhom01_www.aivector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ai-vector")
@ConfigurationProperties(prefix = "app.ai-vector.embedding")
public class VectorEmbeddingProperties {

    private String collectionName = "vector_store";
    private String provider;
    private String model;
    private int dimensions;
    private String configurationVersion;

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getDimensions() { return dimensions; }
    public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    public String getConfigurationVersion() { return configurationVersion; }
    public void setConfigurationVersion(String configurationVersion) { this.configurationVersion = configurationVersion; }

    EmbeddingIdentity identity() {
        return new EmbeddingIdentity(provider, model, dimensions, configurationVersion);
    }
}
