package com.bytehr.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an incoming Bot Framework Activity from Microsoft Teams.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamsActivity {

    @JsonProperty("type")
    private String type;

    @JsonProperty("id")
    private String id;

    @JsonProperty("text")
    private String text;

    @JsonProperty("from")
    private ChannelAccount from;

    @JsonProperty("conversation")
    private ConversationAccount conversation;

    @JsonProperty("channelId")
    private String channelId;

    @JsonProperty("serviceUrl")
    private String serviceUrl;

    @JsonProperty("replyToId")
    private String replyToId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelAccount {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("aadObjectId")
        private String aadObjectId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConversationAccount {
        @JsonProperty("id")
        private String id;

        @JsonProperty("isGroup")
        private Boolean isGroup;
    }
}
