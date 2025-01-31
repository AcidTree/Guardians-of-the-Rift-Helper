package com.datbear.util;

import com.datbear.GuardianInfo;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.runelite.api.ItemID;

/** Static class containing all possible Guardians as instances of GuardianInfo. */
public final class GuardianHelper {
  public static final GuardianInfo AIR =
      new GuardianInfo(1, ItemID.AIR_RUNE, 26887, 4353, false, CellType.Weak);
  public static final GuardianInfo MIND =
      new GuardianInfo(2, ItemID.MIND_RUNE, 26891, 4354, true, CellType.Weak);
  public static final GuardianInfo WATER =
      new GuardianInfo(5, ItemID.WATER_RUNE, 26888, 4355, false, CellType.Medium);
  public static final GuardianInfo EARTH =
      new GuardianInfo(9, ItemID.EARTH_RUNE, 26889, 4356, false, CellType.Strong);
  public static final GuardianInfo FIRE =
      new GuardianInfo(14, ItemID.FIRE_RUNE, 26890, 4357, false, CellType.Overcharged);
  public static final GuardianInfo BODY =
      new GuardianInfo(20, ItemID.BODY_RUNE, 26895, 4358, true, CellType.Weak);
  public static final GuardianInfo COSMIC =
      new GuardianInfo(27, ItemID.COSMIC_RUNE, 26896, 4359, true, CellType.Medium);
  public static final GuardianInfo CHAOS =
      new GuardianInfo(35, ItemID.CHAOS_RUNE, 26892, 4360, true, CellType.Medium);
  public static final GuardianInfo NATURE =
      new GuardianInfo(44, ItemID.NATURE_RUNE, 26897, 4361, true, CellType.Strong);
  public static final GuardianInfo LAW =
      new GuardianInfo(54, ItemID.LAW_RUNE, 26898, 4362, true, CellType.Strong);
  public static final GuardianInfo DEATH =
      new GuardianInfo(65, ItemID.DEATH_RUNE, 26893, 4363, true, CellType.Overcharged);
  public static final GuardianInfo BLOOD =
      new GuardianInfo(77, ItemID.BLOOD_RUNE, 26894, 4364, true, CellType.Overcharged);
  public static final Set<GuardianInfo> ALL =
      ImmutableSet.of(
          AIR, MIND, WATER, EARTH, FIRE, BODY, COSMIC, CHAOS, NATURE, LAW, DEATH, BLOOD);
}
