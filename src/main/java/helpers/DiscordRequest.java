package helpers;

import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;

@SuppressWarnings("rawtypes")
public class DiscordRequest {
    MessageCreateAction messageCreateAction;
    WebhookMessageCreateAction webhookMessageCreateAction;
    AuditableRestAction auditableRestAction;

    public DiscordRequest(MessageCreateAction messageCreateAction) {
        this.messageCreateAction = messageCreateAction;
    }

    public DiscordRequest(WebhookMessageCreateAction webhookMessageCreateAction) {
        this.webhookMessageCreateAction = webhookMessageCreateAction;
    }

    public DiscordRequest(AuditableRestAction auditableRestAction) {
        this.auditableRestAction = auditableRestAction;
    }

    public void complete() {
        if (messageCreateAction != null) {
            messageCreateAction.complete();
        } else if (webhookMessageCreateAction != null) {
            webhookMessageCreateAction.complete();
        } else {
            auditableRestAction.complete();
        }
    }
}
