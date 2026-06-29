package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.model.PlayerCharacter;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.runtime.WindowTitleIdentity;
import com.bot.dhxy.window.runtime.WindowTitleIdentityParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    private final GameClientTracker tracker;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowNativeBindingRefreshService bindingRefreshService;

    public ClientIdentityService(GameClientTracker tracker,
                                 WindowTaskContextHolder windowTaskContextHolder,
                                 WindowNativeBindingRefreshService bindingRefreshService) {
        this.tracker = tracker;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.bindingRefreshService = bindingRefreshService;
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
        Optional<WindowTitleIdentity> identity = WindowTitleIdentityParser.parse(title);
        if (identity.isPresent()) {
            me.setGameServerName(identity.get().server());
            me.setName(identity.get().playerName());
            me.setId(identity.get().playerId());
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
            WindowRuntimeContext runtime = current.get();
            bindingRefreshService.refreshAndCommit(runtime);
            WindowNativeBinding binding = runtime.getNativeBinding();
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
