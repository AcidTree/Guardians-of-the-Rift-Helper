package com.datbear;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

import com.datbear.util.TimerOverlayLocation;
import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

/** Shows the information box. */
public class GuardiansOfTheRiftHelperPanel extends OverlayPanel {
  private final Client client;
  private final GuardiansOfTheRiftHelperPlugin plugin;
  private final GuardiansOfTheRiftHelperConfig config;

  /**
   * Initialises the info box.
   *
   * @param client {@link net.runelite.api.Client}
   * @param plugin {@link GuardiansOfTheRiftHelperPlugin}
   * @param config {@link GuardiansOfTheRiftHelperConfig}
   */
  @Inject
  public GuardiansOfTheRiftHelperPanel(
      final Client client,
      final GuardiansOfTheRiftHelperPlugin plugin,
      final GuardiansOfTheRiftHelperConfig config) {
    super(plugin);
    this.client = client;
    this.plugin = plugin;
    this.config = config;
    setPosition(OverlayPosition.TOP_CENTER);
    getMenuEntries()
        .add(
            new OverlayMenuEntry(
                RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Guardians of the Rift Helper Overlay"));
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if (!config.showPointsOverlay() || (!plugin.isInMainRegion() && !plugin.isInMinigame())) {
      return null;
    }

    Optional<Instant> gameStart = plugin.getNextGameStart();
    if (gameStart.isPresent()) {
      if (config.startTimerOverlayLocation() == TimerOverlayLocation.InfoBox
          || config.startTimerOverlayLocation() == TimerOverlayLocation.Both) {
        int timeToStart = ((int) ChronoUnit.SECONDS.between(Instant.now(), gameStart.get()));
        panelComponent
            .getChildren()
            .add(LineComponent.builder().left("Time to start:").right("" + timeToStart).build());
      }
    } else {
      if (config.inactivePortalOverlayLocation() == TimerOverlayLocation.InfoBox
          || config.inactivePortalOverlayLocation() == TimerOverlayLocation.Both) {
        Optional<Instant> despawn = plugin.getLastPortalDespawnTime();
        int timeSincePortal =
            despawn.isPresent()
                ? ((int) (ChronoUnit.SECONDS.between(despawn.get(), Instant.now())))
                : 0;
        panelComponent
            .getChildren()
            .add(
                LineComponent.builder()
                    .left("Time since portal:")
                    .right("" + timeSincePortal)
                    .rightColor(plugin.getTimeSincePortalColor(timeSincePortal))
                    .build());
      }
    }

    panelComponent
        .getChildren()
        .add(
            LineComponent.builder()
                .left("Reward points:")
                .right(plugin.getElementalRewardPoints() + "/" + plugin.getCatalyticRewardPoints())
                .build());

    if (config.potentialPoints()) {
      final int potElementalPoints = plugin.potentialPointsElemental();
      final int potCatalyticPoints = plugin.potentialPointsCatalytic();
      final int elementalRemain = plugin.getCurrentElementalRewardPoints() % 100;
      final int catalyticRemain = plugin.getCurrentCatalyticRewardPoints() % 100;
      final String potPoints =
          String.format(
              "%d.%02d/%d.%02d",
              potElementalPoints, elementalRemain, potCatalyticPoints, catalyticRemain);
      Color potColor = Color.WHITE;
      if (config.highlightPotential()) {
        potColor =
            potElementalPoints == potCatalyticPoints
                ? config.potentialBalanceColor()
                : config.potentialUnbalanceColor();
      }
      panelComponent
          .getChildren()
          .add(
              LineComponent.builder()
                  .left("Potential:")
                  .rightColor(potColor)
                  .right(potPoints)
                  .build());
    }
    return super.render(graphics);
  }
}
