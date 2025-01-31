package com.datbear;

import com.datbear.util.TimerOverlayLocation;
import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;

/** Timer Overlay. */
public class GuardiansOfTheRiftHelperStartTimerOverlay extends Overlay {
  private final Client client;
  private final GuardiansOfTheRiftHelperPlugin plugin;
  private final GuardiansOfTheRiftHelperConfig config;

  /**
   * Creates timers.
   *
   * @param client {@link net.runelite.api.Client}
   * @param plugin {@link GuardiansOfTheRiftHelperPlugin}
   * @param config {@link GuardiansOfTheRiftHelperConfig}
   */
  @Inject
  public GuardiansOfTheRiftHelperStartTimerOverlay(
      final Client client,
      final GuardiansOfTheRiftHelperPlugin plugin,
      final GuardiansOfTheRiftHelperConfig config) {
    super(plugin);
    this.client = client;
    this.plugin = plugin;
    this.config = config;
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if ((!plugin.isInMainRegion() && !plugin.isInMinigame())) {
      return null;
    }

    if (config.startTimerOverlayLocation() != TimerOverlayLocation.GameOverlay
        && config.startTimerOverlayLocation() != TimerOverlayLocation.Both) {
      return null;
    }

    Optional<Instant> gameStart = plugin.getNextGameStart();

    if (gameStart.isPresent()) {
      int timeToStart = ((int) ChronoUnit.SECONDS.between(Instant.now(), gameStart.get()));

      // fix for showing negative time
      if (timeToStart < 0) {
        return null;
      }

      String mins = String.format("%01d", timeToStart / 60);
      String secs = String.format("%02d", timeToStart % 60);
      String text = mins + ":" + secs;

      int x = 68;
      int y = 23;
      int width = 32;
      int height = 24;
      Rectangle rect = new Rectangle(x, y + height, width, height);

      plugin.drawCenteredString(graphics, text, rect);
    }

    return null;
  }
}
