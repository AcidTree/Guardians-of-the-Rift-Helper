package com.datbear;

import com.datbear.util.CellType;
import com.datbear.util.GuardianHelper;
import com.datbear.util.PointBalance;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class GuardiansOfTheRiftHelperOverlay extends Overlay {
  public static final HashMap<Integer, GuardianInfo> GUARDIAN_INFO =
      new HashMap<Integer, GuardianInfo>() {
        {
          put(43701, GuardianHelper.AIR);
          put(43705, GuardianHelper.MIND);
          put(43702, GuardianHelper.WATER);
          put(43703, GuardianHelper.EARTH);
          put(43704, GuardianHelper.FIRE);
          put(43709, GuardianHelper.BODY);
          put(43710, GuardianHelper.COSMIC);
          put(43706, GuardianHelper.CHAOS);
          put(43711, GuardianHelper.NATURE);
          put(43712, GuardianHelper.LAW);
          put(43707, GuardianHelper.DEATH);
          put(43708, GuardianHelper.BLOOD);
        }
      };
  private static final Color GREEN = new Color(0, 255, 0, 150);
  private static final Color RED = new Color(255, 0, 0, 150);
  private static final int GUARDIAN_TICK_COUNT = 33;
  private static final int PORTAL_TICK_COUNT = 43;

  private static final int RUNE_IMAGE_OFFSET = 505;

  @Inject private ItemManager itemManager;

  @Inject private ModelOutlineRenderer modelOutlineRenderer;

  private final Client client;
  private final GuardiansOfTheRiftHelperPlugin plugin;
  private final GuardiansOfTheRiftHelperConfig config;

  @Inject
  public GuardiansOfTheRiftHelperOverlay(
      Client client, GuardiansOfTheRiftHelperPlugin plugin, GuardiansOfTheRiftHelperConfig config) {
    super();
    setPosition(OverlayPosition.DYNAMIC);
    setLayer(OverlayLayer.ABOVE_SCENE);
    this.client = client;
    this.plugin = plugin;
    this.config = config;
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if (plugin.isInMainRegion()) {
      renderActiveGuardians(graphics);
      highlightGreatGuardian(graphics);
      highlightUnchargedCellTable(graphics);
      highlightDepositPool(graphics);
      highlightEssencePiles(graphics);
      renderPortal(graphics);
    }
    return null;
  }

  private void renderPortal(Graphics2D graphics) {
    if (plugin.getPortalSpawnTime().isPresent() && plugin.getPortal() != null) {
      Instant spawnTime = plugin.getPortalSpawnTime().get();
      GameObject portal = plugin.getPortal();
      long millis =
          ChronoUnit.MILLIS.between(
              Instant.now(), spawnTime.plusMillis((long) Math.floor(PORTAL_TICK_COUNT * 600)));
      String timeRemainingText = "" + (Math.round(millis / 100) / 10d);
      Point textLocation =
          Perspective.getCanvasTextLocation(
              client, graphics, portal.getLocalLocation(), timeRemainingText, 100);
      OverlayUtil.renderTextLocation(graphics, textLocation, timeRemainingText, Color.WHITE);
    }
  }

  private void highlightEssencePiles(Graphics2D graphics) {
    if (plugin.isShouldMakeGuardian()) {
      GameObject elementalEss = plugin.getElementalEssencePile();
      GameObject catalyticEss = plugin.getCatalyticEssencePile();
      if (elementalEss != null) {
        modelOutlineRenderer.drawOutline(elementalEss, 2, config.essencePileColor(), 2);
      }
      if (catalyticEss != null) {
        modelOutlineRenderer.drawOutline(catalyticEss, 2, config.essencePileColor(), 2);
      }
    }
  }

  private CellType bestCell(final Set<GameObject> activeGuardians) {
    CellType best = CellType.Weak;
    for (final GameObject guardian : activeGuardians) {
      if (guardian == null) continue;
      Shape hull = guardian.getConvexHull();
      if (hull == null) continue;
      GuardianInfo info = GUARDIAN_INFO.get(guardian.getId());

      if (info.getCellType().compareTo(best) > 0
          && info.getLevelRequired() < client.getBoostedSkillLevel(Skill.RUNECRAFT)) {
        if (info.getCellType() == CellType.Overcharged) {
          return CellType.Overcharged;
        }
        best = info.getCellType();
      }
    }
    return best;
  }

  private PointBalance currentBalance() {
    PointBalance val = PointBalance.BALANCED;
    final int potElementalPoints = plugin.potentialPointsElemental();
    final int potCatalyticPoints = plugin.potentialPointsCatalytic();
    if (potElementalPoints > potCatalyticPoints) {
      val = PointBalance.NEED_CATALYTIC;
    } else if (potCatalyticPoints > potElementalPoints) {
      val = PointBalance.NEED_ELEMENTAL;
    }
    return val;
  }

  private void renderActiveGuardians(Graphics2D graphics) {
    if (!plugin.isInMainRegion()) return;

    Set<GameObject> activeGuardians = plugin.getActiveGuardians();
    Set<GameObject> guardians = plugin.getGuardians();
    Set<Integer> inventoryTalismans = plugin.getInventoryTalismans();

    PointBalance balance = PointBalance.BALANCED;
    CellType bestCell = null;

    if (config.pointBalanceHelper()) {
      balance = currentBalance();
    }

    for (GameObject guardian : activeGuardians) {
      if (guardian == null) continue;
      Shape hull = guardian.getConvexHull();
      if (hull == null) continue;

      GuardianInfo info = GUARDIAN_INFO.get(guardian.getId());

      if (config.hideHighLvl()
          && info.getLevelRequired() > client.getBoostedSkillLevel(Skill.RUNECRAFT)) {
        continue;
      }

      if (config.pointBalanceHelper()) {
        if (!info.isCatalytic() && balance == PointBalance.NEED_CATALYTIC) {
          continue;
        } else if (info.isCatalytic() && balance == PointBalance.NEED_ELEMENTAL) {
          continue;
        } else if (balance == PointBalance.BALANCED) {
          if (bestCell == null) {
            bestCell = bestCell(activeGuardians);
          }
          if (info.getCellType() != bestCell) {
            continue;
          }
        }
      }

      Color color = info.getColor(config);
      graphics.setColor(color);

      if (config.guardianOutline()) {
        modelOutlineRenderer.drawOutline(
            guardian, config.guardianBorderWidth(), color, config.guardianOutlineFeather());
      }

      BufferedImage img = info.getRuneImage(itemManager);
      OverlayUtil.renderImageLocation(
          client, graphics, guardian.getLocalLocation(), img, RUNE_IMAGE_OFFSET);
      if (!info.getSpawnTime().isPresent()) continue;

      Point imgLocation =
          Perspective.getCanvasImageLocation(
              client, guardian.getLocalLocation(), img, RUNE_IMAGE_OFFSET);
      long millis =
          ChronoUnit.MILLIS.between(
              Instant.now(),
              info.getSpawnTime().get().plusMillis((long) Math.floor(GUARDIAN_TICK_COUNT * 600)));
      String timeRemainingText = "" + (Math.round(millis / 100) / 10d);
      Rectangle2D strBounds =
          graphics.getFontMetrics().getStringBounds(timeRemainingText, graphics);
      Point textLocation =
          Perspective.getCanvasTextLocation(
              client,
              graphics,
              guardian.getLocalLocation(),
              timeRemainingText,
              RUNE_IMAGE_OFFSET + 60);
      if (textLocation == null) continue;

      textLocation =
          new Point(
              (int) (imgLocation.getX() + img.getWidth() / 2d - strBounds.getWidth() / 2d),
              textLocation.getY());
      OverlayUtil.renderTextLocation(graphics, textLocation, timeRemainingText, Color.WHITE);
    }

    for (int talisman : inventoryTalismans) {
      Optional<GameObject> talismanGuardian =
          guardians.stream()
              .filter(x -> GUARDIAN_INFO.get(x.getId()).getTalismanId() == talisman)
              .findFirst();

      if (talismanGuardian.isPresent()
          && activeGuardians.stream().noneMatch(x -> x.getId() == talismanGuardian.get().getId())) {
        GuardianInfo talismanGuardianInfo = GUARDIAN_INFO.get(talismanGuardian.get().getId());
        if (config.guardianOutline()) {
          modelOutlineRenderer.drawOutline(
              talismanGuardian.get(),
              config.guardianBorderWidth(),
              talismanGuardianInfo.getColor(config),
              config.guardianOutlineFeather());
        }
        OverlayUtil.renderImageLocation(
            client,
            graphics,
            talismanGuardian.get().getLocalLocation(),
            talismanGuardianInfo.getTalismanImage(itemManager),
            RUNE_IMAGE_OFFSET);
      }
    }
  }

  private void highlightGreatGuardian(Graphics2D graphics) {
    if (!config.outlineGreatGuardian()) {
      return;
    }

    NPC greatGuardian = plugin.getGreatGuardian();
    if (plugin.isOutlineGreatGuardian() && greatGuardian != null) {
      modelOutlineRenderer.drawOutline(greatGuardian, 2, Color.GREEN, 2);
    }
  }

  private void highlightUnchargedCellTable(Graphics2D graphics) {
    if (!config.outlineCellTable()) {
      return;
    }

    GameObject table = plugin.getUnchargedCellTable();
    if (plugin.isOutlineUnchargedCellTable() && table != null) {
      modelOutlineRenderer.drawOutline(table, 2, GREEN, 2);
    }
  }

  private void highlightDepositPool(Graphics2D graphics) {
    if (!config.outlineDepositPool()) {
      return;
    }

    GameObject depositPool = plugin.getDepositPool();
    if (plugin.isOutlineDepositPool() && depositPool != null) {
      modelOutlineRenderer.drawOutline(depositPool, 2, GREEN, 2);
    }
  }
}
