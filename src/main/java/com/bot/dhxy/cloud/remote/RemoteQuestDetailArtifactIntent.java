package com.bot.dhxy.cloud.remote;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Immutable quest-detail artifact intent carrying only the closed task code.
 *
 * <p>No default value, path, kind, free text, or compatibility fallback exists here; the Cloud
 * mirror DTO is {@code com.yueyunfe.dhxy.cloudbrain.remote.QuestDetailArtifactIntent}.</p>
 */
@Value
@Builder
@Jacksonized
public class RemoteQuestDetailArtifactIntent {
    RemoteQuestArtifactTaskCode taskCode;
}
