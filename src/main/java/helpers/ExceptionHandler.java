package helpers;

import controller.DiscordGame;
import exceptions.ChannelNotFoundException;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for handling exceptions that occur during game interactions.
 * Sends exception details to the #mod-info channel in the game category.
 */
public class ExceptionHandler {

    /**
     * Sends an exception report to the #mod-info channel in the game category.
     * The exception message is sent as a Discord message, and the full stack trace
     * is attached as a text file.
     *
     * @param category The game category where the exception occurred
     * @param exception The exception that was thrown
     * @param context Additional context about where the exception occurred (e.g., "Button press: storm-advance")
     * @param user The user who triggered the event that caused the exception (can be null)
     */
    public static void sendExceptionToModInfo(Category category, Throwable exception, String context, User user) {
        if (category == null) {
            System.err.println("Cannot send exception to mod-info: category is null");
            exception.printStackTrace();
            return;
        }

        try {
            DiscordGame discordGame = new DiscordGame(category);

            // Create the error message
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String userInfo = user != null ? "\n**User:** " + user.getAsTag() : "";
            String errorMessage = String.format(
                "**Error occurred at %s**\n**Context:** %s%s\n**Exception:** %s: %s",
                timestamp,
                context,
                userInfo,
                exception.getClass().getSimpleName(),
                exception.getMessage() != null ? exception.getMessage() : "(no message)"
            );

            // Create the stack trace file
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String stackTrace = sw.toString();

            String filename = String.format(
                "error-%s-%s.txt",
                timestamp.replace(" ", "_").replace(":", "-"),
                exception.getClass().getSimpleName()
            );

            FileUpload fileUpload = FileUpload.fromData(
                stackTrace.getBytes(StandardCharsets.UTF_8),
                filename
            );

            // Send directly to mod-info channel (don't use queue since game may not be loaded)
            discordGame.getTextChannel("mod-info")
                .sendMessage(errorMessage)
                .addFiles(fileUpload)
                .queue();

        } catch (ChannelNotFoundException e) {
            System.err.println("Could not find mod-info channel to send exception report");
            System.err.println("Original exception:");
            exception.printStackTrace();
            System.err.println("Channel not found exception:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error while trying to send exception to mod-info channel");
            System.err.println("Original exception:");
            exception.printStackTrace();
            System.err.println("Error sending to mod-info:");
            e.printStackTrace();
        }
    }

}
