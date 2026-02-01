package dashketch.apps.modpackmaker.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VersionService {
    private final OkHttpClient client = new OkHttpClient();

    public List<String> getMinecraftVersions() throws IOException {
        Request request = new Request.Builder()
                .url("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Failed to fetch versions");

            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray versions = json.getAsJsonArray("versions");
            List<String> releaseVersions = new ArrayList<>();

            for (int i = 0; i < versions.size(); i++) {
                JsonObject v = versions.get(i).getAsJsonObject();
                // We only want stable releases, not snapshots
                if (v.get("type").getAsString().equals("release")) {
                    releaseVersions.add(v.get("id").getAsString());
                }
            }
            return releaseVersions;
        }
    }
}