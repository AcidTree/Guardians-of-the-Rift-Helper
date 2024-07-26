package com.datbear;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Optional;
import lombok.Getter;
import net.runelite.client.game.ItemManager;

/** Contains all information of a guardian. */
@Getter
public class GuardianInfo {
  private final int levelRequired;
  private final int runeId;
  private final int talismanId;
  private final int spriteId;
  private final boolean isCatalytic;
  private final CellType cellType;
  private Optional<Instant> spawnTime = Optional.empty();

  /**
   * @param levelRequired Level required by player to access guardian
   * @param runeId Rune associated with the guardian
   * @param talismanId Talisman used to access guardian
   * @param spriteId Sprite of the guardian
   * @param isCatalytic If the guardian is catalytic
   * @param cellType Type of cell the guardian provides
   */
  public GuardianInfo(
      int levelRequired,
      int runeId,
      int talismanId,
      int spriteId,
      boolean isCatalytic,
      CellType cellType) {
    this.levelRequired = levelRequired;
    this.runeId = runeId;
    this.talismanId = talismanId;
    this.spriteId = spriteId;
    this.isCatalytic = isCatalytic;
    this.cellType = cellType;
  }

  public BufferedImage getRuneImage(ItemManager itemManager) {
    return itemManager.getImage(getRuneId());
  }

  public BufferedImage getTalismanImage(ItemManager itemManager) {
    return itemManager.getImage(getTalismanId());
  }

  public void spawn() {
    spawnTime = Optional.of(Instant.now());
  }

  public void despawn() {
    spawnTime = Optional.empty();
  }

  /**
   * @param config instance of config to lookup colours.
   * @return Returns color defined in config to highlight guardian.
   */
  public Color getColor(GuardiansOfTheRiftHelperConfig config) {
    if (config.colorGuardiansByTier()) {
      switch (getCellType()) {
        case Weak:
          return config.weakGuardianColor();
        case Medium:
          return config.mediumGuardianColor();
        case Strong:
          return config.strongGuardianColor();
        case Overcharged:
          return config.overchargedGuardianColor();
      }
    } else {
      return isCatalytic() ? config.catalyticGuardianColor() : config.elementalGuardianColor();
    }

    return Color.WHITE;
  }
}
