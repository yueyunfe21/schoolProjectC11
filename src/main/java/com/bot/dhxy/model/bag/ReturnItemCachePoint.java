package com.bot.dhxy.model.bag;

import lombok.Builder;
import lombok.Value;

/**
 * Screen-absolute click point learned for a task return item in the current bound window.
 *
 * @param templatePath item template path relative to {@code images/template/}.
 * @param clickX screen-absolute raw item point before the final safe click jitter.
 * @param clickY screen-absolute raw item point before the final safe click jitter.
 * @param learnedAtMs wall-clock time when the point was found.
 * @param source diagnostic source that produced this cache point.
 */
@Value
@Builder
public class ReturnItemCachePoint {
    String templatePath;
    int clickX;
    int clickY;
    long learnedAtMs;
    String source;
}
