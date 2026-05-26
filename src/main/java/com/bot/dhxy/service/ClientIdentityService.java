package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronizes the current player's identity from the bound native game-window title.
 *
 * <p>The service is window-binding aware. Multi-window task execution must prefer the title from
 * the current {@link WindowRuntimeContext}, because the tracker title can lag during startup and
 * debug runs. This service mutates the supplied {@link PlayerCharacter} only; it does not send
 * input, capture screenshots, or call OCR.</p>
 */
@Slf4j
@Service
public class ClientIdentityService {

    /**
     * Matches titles such as:
     * 大话西游2经典版 $Revision: 2020549 - 江山如画 - 刑部ヾ忍者（ID：67555）
     *
     * <p>Both ASCII and Chinese parentheses/colons are accepted because the title text can be
     * produced by different Windows/input-method encodings.</p>
     */
    private static final Pattern TITLE_IDENTITY_PATTERN = Pattern.compile(
            "-\\s*(.+?)\\s*-\\s*(.+?)\\s*[（(]\\s*ID\\s*[:：]\\s*(\\d+)\\s*[）)]");

    private final GameClientTracker tracker;
    private final WindowTaskContextHolder windowTaskContextHolder;

    public ClientIdentityService(GameClientTracker tracker,
                                 WindowTaskContextHolder windowTaskContextHolder) {
        this.tracker = tracker;
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

    /**
     * Parse server/name/id from the current bound window title and write them into player state.
     *
     * @param me current per-window player state to mutate; null means no state is available and the
     *           sync is skipped.
     */
    public void scanAndSyncIdentity(PlayerCharacter me) {
        if (me == null) {
            log.warn("[identity] skip sync: player state is null");
            return;
        }

        String title = resolveCurrentWindowTitle();
        if (title == null || title.isBlank()) {
            log.warn("[identity] current window title is blank; cannot parse player identity");
            return;
        }

        log.info("[identity] parse player identity from title: {}", title);
        Matcher matcher = TITLE_IDENTITY_PATTERN.matcher(title);
        if (matcher.find()) {
            me.setGameServerName(matcher.group(1));
            me.setName(matcher.group(2));
            me.setId(matcher.group(3));
            log.info("[identity] parsed player identity: server={} name={} id={}",
                    me.getGameServerName(), me.getName(), me.getId());
        } else {
            log.warn("[identity] title did not match expected player identity format: {}", title);
        }
    }

    /**
     * Resolve the title in the same priority order as the multi-window architecture.
     *
     * @return native title from current window binding, tracker cache, or locate-window fallback;
     * null when no title can be found.
     */
    private String resolveCurrentWindowTitle() {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isPresent()) {
            WindowNativeBinding binding = current.get().getNativeBinding();
            if (binding != null && binding.getTitle() != null && !binding.getTitle().isBlank()) {
                return binding.getTitle();
            }
        }

        String trackerTitle = tracker.getFullWindowTitle();
        if (trackerTitle != null && !trackerTitle.isBlank()) {
            return trackerTitle;
        }

        if (tracker.locateWindow()) {
            trackerTitle = tracker.getFullWindowTitle();
            if (trackerTitle != null && !trackerTitle.isBlank()) {
                return trackerTitle;
            }
        }

        return null;
    }
}
