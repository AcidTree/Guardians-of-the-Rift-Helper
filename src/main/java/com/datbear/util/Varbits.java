package com.datbear.util;

/**
 * Server controlled "content-developer" integers.
 *
 * @see net.runelite.api.Varbits
 */
public final class Varbits {
  /** Elemental points gained in current GOTR game. */
  public static final int ELEMENTAL_POINTS = 13686;

  /** Catalytic points gained in current GOTR game. */
  public static final int CATALYTIC_POINTS = 13685;

  /**
   * If the player is in the gotr zone.
   *
   * <p>0 = false <br>
   * 1 = true
   */
  public static final int GOTR_ARENA = 13691;

  /**
   * If the player is in an active gotr game.
   *
   * <p>0 = false <br>
   * * 1 = true
   */
  public static final int GOTR_ENDED = 13688;

  /**
   * If the player is in the portal mining area.
   *
   * <p>0 = false <br>
   * 1 = true
   */
  public static final int PORTAL_AREA = 13687;
}
