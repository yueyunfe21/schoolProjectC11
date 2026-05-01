package com.bot.dhxy.core;

import com.bot.dhxy.model.PlayerCharacter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * 游戏的“内存条”：只负责存数据，没有任何业务逻辑！
 */
@Component
public class GameContext {
    // 一开始我不知道是谁上线了，所以档案是空的
    @Getter
    @Setter
    private PlayerCharacter me = new PlayerCharacter();

}