package com.datbear;

import com.datbear.util.GuardianHelper;
import com.datbear.util.Varbits;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/** Main class for the GOTR Helper. */
@Slf4j
@PluginDescriptor(
    name = "Guardians of the Rift Helper",
    description = "Show info about the Guardians of the Rift minigame",
    tags = {"minigame", "overlay", "guardians of the rift"})
public class GuardiansOfTheRiftHelperPlugin extends Plugin {
  private static final int MINIGAME_MAIN_REGION = 14484;
  private static final Set<Integer> GUARDIAN_IDS =
      ImmutableSet.of(
          43705, 43701, 43710, 43702, 43703, 43711, 43704, 43708, 43712, 43707, 43706, 43709,
          43702);
  private static final Set<Integer> RUNE_IDS =
      ImmutableSet.of(
          554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 4694, 4695, 4696, 4697, 4698,
          4699);
  private static final Set<Integer> TALISMAN_IDS =
      GuardianHelper.ALL.stream()
          .mapToInt(GuardianInfo::getTalismanId)
          .boxed()
          .collect(Collectors.toSet());
  private static final int GREAT_GUARDIAN_ID = 11403;
  private static final int CATALYTIC_GUARDIAN_STONE_ID = 26880;
  private static final int ELEMENTAL_GUARDIAN_STONE_ID = 26881;
  private static final int POLYELEMENTAL_GUARDIAN_STONE_ID = 26941;
  private static final int ELEMENTAL_ESSENCE_PILE_ID = 43722;
  private static final int CATALYTIC_ESSENCE_PILE_ID = 43723;
  private static final int UNCHARGED_CELL_ITEM_ID = 26882;
  private static final int UNCHARGED_CELL_GAMEOBJECT_ID = 43732;
  private static final int CHISEL_ID = 1755;
  private static final int OVERCHARGED_CELL_ID = 26886;
  private static final int DEPOSIT_POOL_ID = 43696;
  private static final int GUARDIAN_ACTIVE_ANIM = 9363;
  private static final int PARENT_WIDGET_ID = 48889857;
  private static final int CATALYTIC_RUNE_WIDGET_ID = 48889876;
  private static final int ELEMENTAL_RUNE_WIDGET_ID = 48889879;
  private static final int GUARDIAN_COUNT_WIDGET_ID = 48889886;
  private static final int PORTAL_WIDGET_ID = 48889884;
  private static final int PORTAL_SPRITE_ID = 4368;
  private static final int PORTAL_ID = 43729;
  private static final int LOCKED_BARRIER_ID = 43849;
  private static final int UNLOCKED_BARRIER_ID = 43700;
  private static final String REWARD_POINT_REGEX =
      "Total elemental energy:[^>]+>([\\d,]+).*Total catalytic energy:[^>]+>([\\d,]+).";
  private static final Pattern REWARD_POINT_PATTERN = Pattern.compile(REWARD_POINT_REGEX);
  private static final String CHECK_POINT_REGEX =
      "You have (\\d+) catalytic energy and (\\d+) elemental energy";
  private static final Pattern CHECK_POINT_PATTERN = Pattern.compile(CHECK_POINT_REGEX);
  private static final int DIALOG_WIDGET_GROUP = 229;
  private static final int GUARDIAN_PERCENT_WIDGET =
      48889874; // parent 48889860 < 48889859 < 48889858
  private static final int TIMER_WIDGET = 48889861;
  private static final Pattern TIMER_PATTERN = Pattern.compile("(\\d?\\d):(\\d?\\d)");
  private static final Pattern PERCENT_PATTERN = Pattern.compile(".+: (\\d?\\d)%");
  private static final int DIALOG_WIDGET_MESSAGE = 1;
  private static final String BARRIER_DIALOG_FINISHING_UP =
      "It looks like the adventurers within are just finishing up. You must<br>wait until they are done to join.";

  @Getter(AccessLevel.PACKAGE)
  private final Set<GameObject> guardians = new HashSet<>();

  @Getter(AccessLevel.PACKAGE)
  private final Set<GameObject> activeGuardians = new HashSet<>();

  @Getter(AccessLevel.PACKAGE)
  private final Set<Integer> inventoryTalismans = new HashSet<>();

  private final Map<String, String> expandCardinal = new HashMap<>();
  @Inject private Client client;
  @Inject private GuardiansOfTheRiftHelperConfig config;
  @Inject private OverlayManager overlayManager;
  @Inject private GuardiansOfTheRiftHelperOverlay overlay;
  @Inject private GuardiansOfTheRiftHelperPanel panel;
  @Inject private GuardiansOfTheRiftHelperStartTimerOverlay startTimerOverlay;
  @Inject private GuardiansOfTheRiftHelperInactivePortalOverlay inactivePortalOverlay;
  @Inject private Notifier notifier;
  private GuardianInfo guardianInfo;

  @Getter(AccessLevel.PACKAGE)
  private NPC greatGuardian;

  @Getter(AccessLevel.PACKAGE)
  private GameObject unchargedCellTable;

  @Getter(AccessLevel.PACKAGE)
  private GameObject depositPool;

  @Getter(AccessLevel.PACKAGE)
  private GameObject catalyticEssencePile;

  @Getter(AccessLevel.PACKAGE)
  private GameObject elementalEssencePile;

  @Getter(AccessLevel.PACKAGE)
  private GameObject portal;

  @Getter(AccessLevel.PACKAGE)
  private boolean isInMinigame;

  @Getter(AccessLevel.PACKAGE)
  private boolean activeGame;

  /**
   * set by {@link #checkInMainRegion()} each tick.
   *
   * @see #checkInMainRegion()
   * @see #onGameTick(GameTick)
   */
  @Getter(AccessLevel.PACKAGE)
  private boolean isInMainRegion;

  @Getter(AccessLevel.PACKAGE)
  private boolean inSideArea;

  @Getter(AccessLevel.PACKAGE)
  private boolean outlineGreatGuardian = false;

  @Getter(AccessLevel.PACKAGE)
  private boolean outlineUnchargedCellTable = false;

  @Getter(AccessLevel.PACKAGE)
  private boolean outlineDepositPool = false;

  @Getter(AccessLevel.PACKAGE)
  private boolean shouldMakeGuardian = false;

  @Getter(AccessLevel.PACKAGE)
  private boolean isFirstPortal = false;

  @Getter(AccessLevel.PACKAGE)
  private int elementalRewardPoints;

  @Getter(AccessLevel.PACKAGE)
  private int catalyticRewardPoints;

  @Getter(AccessLevel.PACKAGE)
  private int currentElementalRewardPoints;

  @Getter(AccessLevel.PACKAGE)
  private int currentCatalyticRewardPoints;

  @Getter(AccessLevel.PACKAGE)
  private Optional<Instant> portalSpawnTime = Optional.empty();

  @Getter(AccessLevel.PACKAGE)
  private Optional<Instant> lastPortalDespawnTime = Optional.empty();

  @Getter(AccessLevel.PACKAGE)
  private Optional<Instant> nextGameStart = Optional.empty();

  @Getter(AccessLevel.PACKAGE)
  private int lastRewardUsage;

  private String portalLocation;
  private int lastElementalRuneSprite;
  private int lastCatalyticRuneSprite;
  private boolean areGuardiansNeeded = false;
  private int entryBarrierClickCooldown = 0;
  // entryBarrierIsLocked == Empty -> Barrier has not yet been seen
  //                      == True  -> Barrier was last seen locked
  //                      == False -> Barrier was last seen unlocked
  private Optional<Boolean> entryBarrierIsLocked = Optional.empty();
  private boolean sentTimer = false;

  /**
   * Checks if in anywhere gotr area/map.
   *
   * @return true if in GOTR map area
   */
  private boolean checkInMainRegion() {
    int[] currentMapRegions = client.getMapRegions();
    return Arrays.stream(currentMapRegions).anyMatch(x -> x == MINIGAME_MAIN_REGION);
  }

  /**
   * Checks is player is in active game.
   *
   * @return true if in active game
   */
  public boolean isPlayingGame() {
    return activeGame && isInMinigame;
  }

  @Override
  protected void startUp() {
    overlayManager.add(overlay);
    overlayManager.add(panel);
    overlayManager.add(startTimerOverlay);
    overlayManager.add(inactivePortalOverlay);
    isInMinigame = true;
    expandCardinal.put("S", "south");
    expandCardinal.put("SW", "south west");
    expandCardinal.put("W", "west");
    expandCardinal.put("NW", "north west");
    expandCardinal.put("N", "north");
    expandCardinal.put("NE", "north east");
    expandCardinal.put("E", "east");
    expandCardinal.put("SE", "south east");
    loadPoints();
  }

  @Override
  protected void shutDown() {
    overlayManager.remove(overlay);
    overlayManager.remove(panel);
    overlayManager.remove(startTimerOverlay);
    overlayManager.remove(inactivePortalOverlay);
    reset();
  }

  /**
   * Checks inventory for highlighting npcs/objects.
   *
   * @param event {@link net.runelite.api.events.ItemContainerChanged}
   */
  @Subscribe
  public void onItemContainerChanged(ItemContainerChanged event) {
    if (!isInMainRegion
        || event.getItemContainer() != client.getItemContainer(InventoryID.INVENTORY)) {
      return;
    }

    Item[] items = event.getItemContainer().getItems();
    outlineGreatGuardian =
        Arrays.stream(items)
            .anyMatch(
                x ->
                    x.getId() == ELEMENTAL_GUARDIAN_STONE_ID
                        || x.getId() == CATALYTIC_GUARDIAN_STONE_ID
                        || x.getId() == POLYELEMENTAL_GUARDIAN_STONE_ID);
    checkCellTable(items);
    shouldMakeGuardian =
        Arrays.stream(items).anyMatch(x -> x.getId() == CHISEL_ID)
            && Arrays.stream(items).anyMatch(x -> x.getId() == OVERCHARGED_CELL_ID)
            && areGuardiansNeeded;

    outlineDepositPool = Arrays.stream(items).anyMatch(x -> RUNE_IDS.contains(x.getId()));

    List<Integer> invTalismans =
        Arrays.stream(items)
            .mapToInt(Item::getId)
            .filter(TALISMAN_IDS::contains)
            .boxed()
            .collect(Collectors.toList());
    if ((long) invTalismans.size() != (long) inventoryTalismans.size()) {
      inventoryTalismans.clear();
      inventoryTalismans.addAll(invTalismans);
    }
  }

  /**
   * Monitors game state each tick.
   *
   * @param tick {@link net.runelite.api.events.GameTick}
   */
  @Subscribe
  public void onGameTick(GameTick tick) {
    isInMainRegion = checkInMainRegion();
    if (entryBarrierClickCooldown > 0) {
      entryBarrierClickCooldown--;
    }

    activeGuardians.removeIf(
        ag -> {
          Animation anim = ((DynamicObject) ag.getRenderable()).getAnimation();
          return anim == null || anim.getId() != GUARDIAN_ACTIVE_ANIM;
        });

    for (GameObject guardian : guardians) {
      Animation animation = ((DynamicObject) guardian.getRenderable()).getAnimation();
      if (animation != null && animation.getId() == GUARDIAN_ACTIVE_ANIM) {
        activeGuardians.add(guardian);
      }
    }

    Widget elementalRuneWidget = client.getWidget(ELEMENTAL_RUNE_WIDGET_ID);
    Widget catalyticRuneWidget = client.getWidget(CATALYTIC_RUNE_WIDGET_ID);
    Widget guardianCountWidget = client.getWidget(GUARDIAN_COUNT_WIDGET_ID);
    Widget portalWidget = client.getWidget(PORTAL_WIDGET_ID);

    lastElementalRuneSprite = parseRuneWidget(elementalRuneWidget, lastElementalRuneSprite);
    lastCatalyticRuneSprite = parseRuneWidget(catalyticRuneWidget, lastCatalyticRuneSprite);

    if (guardianCountWidget != null) {
      String text = guardianCountWidget.getText();
      areGuardiansNeeded = text != null && !text.contains("10/10");
    }

    if (portalWidget != null && !portalWidget.isHidden()) {
      if (portalSpawnTime.isEmpty() && lastPortalDespawnTime.isPresent()) {
        lastPortalDespawnTime = Optional.empty();
        if (isFirstPortal) {
          isFirstPortal = false;
        }
        if (config.notifyPortalSpawn()) {
          String compass = portalWidget.getText().split(" ")[0];
          String full = expandCardinal.getOrDefault(compass, "unknown");
          notifier.notify("A portal has spawned in the " + full + ".");
        }
      }
      portalLocation = portalWidget.getText();
      portalSpawnTime = portalSpawnTime.isPresent() ? portalSpawnTime : Optional.of(Instant.now());
    } else if (elementalRuneWidget != null && !elementalRuneWidget.isHidden()) {
      if (portalSpawnTime.isPresent()) {
        lastPortalDespawnTime = Optional.of(Instant.now());
      }
      portalLocation = null;
      portalSpawnTime = Optional.empty();
    }

    Widget dialog = client.getWidget(DIALOG_WIDGET_GROUP, DIALOG_WIDGET_MESSAGE);
    if (dialog != null) {
      String dialogText = dialog.getText();
      if (dialogText.equals(BARRIER_DIALOG_FINISHING_UP)) {
        // Allow one click per tick while the portal is closed
        entryBarrierClickCooldown = 0;
      } else {
        final Matcher checkMatcher = CHECK_POINT_PATTERN.matcher(dialogText);
        if (checkMatcher.find(0)) {
          // For some reason these are reversed compared to everything else
          catalyticRewardPoints = Integer.parseInt(checkMatcher.group(1));
          elementalRewardPoints = Integer.parseInt(checkMatcher.group(2));
        }
      }
    }

    if (config.timerNotify() > 0 && !sentTimer) {
      Widget timer = client.getWidget(TIMER_WIDGET);
      Widget percent = client.getWidget(GUARDIAN_PERCENT_WIDGET);
      if (timer != null && percent != null) {
        Matcher p = PERCENT_PATTERN.matcher(percent.getText());
        Matcher t = TIMER_PATTERN.matcher(timer.getText());
        if (p.matches() && t.matches()) {
          if (Integer.parseInt(p.group(1)) == 10) {
            if (((Integer.parseInt(t.group(1)) * 60) + Integer.parseInt(t.group(2)))
                < config.timerNotify()) {
              notifier.notify("Timer reached");
              sentTimer = true;
            }
          } else if (Integer.parseInt(p.group(1)) > 10) {
            sentTimer = true;
          }
        }
      }
    }
  }

  int parseRuneWidget(Widget runeWidget, int lastSpriteId) {
    if (runeWidget != null) {
      int spriteId = runeWidget.getSpriteId();
      if (spriteId != lastSpriteId) {
        if (lastSpriteId > 0) {
          Optional<GuardianInfo> lastGuardian =
              GuardianHelper.ALL.stream().filter(g -> g.getSpriteId() == lastSpriteId).findFirst();
          if (lastGuardian.isPresent()) {
            lastGuardian.get().despawn();
          }
        }

        Optional<GuardianInfo> currentGuardian =
            GuardianHelper.ALL.stream().filter(g -> g.getSpriteId() == spriteId).findFirst();
        if (currentGuardian.isPresent()) {
          currentGuardian.get().spawn();
        }
      }

      return spriteId;
    }
    return lastSpriteId;
  }

  /**
   * Gets game objects on spawn.
   *
   * @param event {@link net.runelite.api.events.GameObjectSpawned}
   */
  @Subscribe
  public void onGameObjectSpawned(GameObjectSpawned event) {
    GameObject gameObject = event.getGameObject();
    if (GUARDIAN_IDS.contains(event.getGameObject().getId())) {
      guardians.removeIf(g -> g.getId() == gameObject.getId());
      activeGuardians.removeIf(g -> g.getId() == gameObject.getId());
      guardians.add(gameObject);
    }

    if (gameObject.getId() == UNCHARGED_CELL_GAMEOBJECT_ID) {
      unchargedCellTable = gameObject;
    }

    if (gameObject.getId() == DEPOSIT_POOL_ID) {
      depositPool = gameObject;
    }

    if (gameObject.getId() == ELEMENTAL_ESSENCE_PILE_ID) {
      elementalEssencePile = gameObject;
    }

    if (gameObject.getId() == CATALYTIC_ESSENCE_PILE_ID) {
      catalyticEssencePile = gameObject;
    }

    if (gameObject.getId() == PORTAL_ID) {
      portal = gameObject;
      if (config.notifyPortalSpawn()) {
        // The hint arrow is cleared under the following circumstances:
        // 1. Player enters the portal
        // 2. Plugin is "reset()"
        // 3. The portal despawns
        client.setHintArrow(portal.getWorldLocation());
      }
    }
    if (gameObject.getId() == LOCKED_BARRIER_ID) {
      entryBarrierIsLocked = Optional.of(true);
    }
    if (gameObject.getId() == UNLOCKED_BARRIER_ID) {
      entryBarrierIsLocked = Optional.of(false);
    }
  }

  /**
   * Checks barrier locks and portal changes.
   *
   * @param event {@link net.runelite.api.events.GameObjectDespawned}
   */
  @Subscribe
  public void onGameObjectDespawned(GameObjectDespawned event) {
    // Portal monitoring
    if (event.getGameObject().getId() == PORTAL_ID) {
      client.clearHintArrow();
    }

    // Barrier state monitoring
    if (event.getGameObject().getId() == LOCKED_BARRIER_ID) {
      if (entryBarrierIsLocked.orElse(false)) {
        notifier.notify("Rift Barrier is about to unlock!");
      }
      entryBarrierIsLocked = Optional.of(false);
    }
    if (event.getGameObject().getId() == UNLOCKED_BARRIER_ID) {
      entryBarrierIsLocked = Optional.of(true);
    }
  }

  /**
   * Watches for great guardian spawn.
   *
   * @param npcSpawned {@link net.runelite.api.events.NpcSpawned}
   */
  @Subscribe
  public void onNpcSpawned(NpcSpawned npcSpawned) {
    NPC npc = npcSpawned.getNpc();
    if (npc.getId() == GREAT_GUARDIAN_ID) {
      greatGuardian = npc;
    }
  }

  /**
   * Watches for login screen or loading.
   *
   * @param event {@link net.runelite.api.events.GameStateChanged}
   */
  @Subscribe
  public void onGameStateChanged(GameStateChanged event) {
    switch (event.getGameState()) {
      case LOADING:
        reset();
        break;
      case LOGIN_SCREEN:
        isInMinigame = false;
        break;
        //      case LOGGED_IN:
        //        log.debug("Logged in");
        //        outlineUnchargedCellTable = true;
        //        break;
      default:
    }
  }

  private void checkCellTable(Item[] items) {
    outlineUnchargedCellTable =
        Arrays.stream(items).noneMatch(x -> x.getId() == UNCHARGED_CELL_ITEM_ID);
  }

  private void checkCellTable() {
    ItemContainer i = client.getItemContainer(InventoryID.INVENTORY);
    if (i != null) {
      checkCellTable(i.getItems());
    }
  }

  /**
   * Updates key variables on varbit change.
   *
   * @param e {@link net.runelite.api.events.VarbitChanged}
   */
  @Subscribe
  public void onVarbitChanged(VarbitChanged e) {
    if (!isInMainRegion) {
      return;
    }
    switch (e.getVarbitId()) {
      case Varbits.ELEMENTAL_POINTS:
        currentElementalRewardPoints = e.getValue();
        break;
      case Varbits.CATALYTIC_POINTS:
        currentCatalyticRewardPoints = e.getValue();
        break;
      case Varbits.GOTR_ARENA:
        isInMinigame = e.getValue() == 1;
        if (isInMinigame) {
          checkCellTable();
        }
        break;
      case Varbits.GOTR_ENDED:
        activeGame = e.getValue() == 0;
        break;
      case Varbits.PORTAL_AREA:
        inSideArea = e.getValue() == 1;
        break;
      default:
    }
  }

  /**
   * Watches messages for game state changes.
   *
   * @param chatMessage {@link net.runelite.api.events.ChatMessage}
   */
  @Subscribe
  public void onChatMessage(ChatMessage chatMessage) {
    if (!isInMainRegion) {
      return;
    }
    if (chatMessage.getType() != ChatMessageType.SPAM
        && chatMessage.getType() != ChatMessageType.GAMEMESSAGE) {
      return;
    }

    String msg = chatMessage.getMessage();
    if (msg.contains("You step through the portal")) {
      client.clearHintArrow();
    }
    if (msg.contains("The rift becomes active!")) {
      lastPortalDespawnTime = Optional.of(Instant.now());
      nextGameStart = Optional.empty();
      isFirstPortal = true;
      sentTimer = false;
    } else if (msg.contains("The rift will become active in 30 seconds.")) {
      nextGameStart = Optional.of(Instant.now().plusSeconds(30));
    } else if (msg.contains("The rift will become active in 10 seconds.")) {
      nextGameStart = Optional.of(Instant.now().plusSeconds(10));
    } else if (msg.contains("The rift will become active in 5 seconds.")) {
      nextGameStart = Optional.of(Instant.now().plusSeconds(5));
    } else if (msg.contains(
        "The Portal Guardians will keep their rifts open for another 30 seconds.")) {
      nextGameStart = Optional.of(Instant.now().plusSeconds(60));
    } else if (msg.contains("You found some loot:")) {
      elementalRewardPoints--;
      catalyticRewardPoints--;
    }

    Matcher rewardPointMatcher = REWARD_POINT_PATTERN.matcher(msg);
    if (rewardPointMatcher.find()) {
      // Use replaceAll to remove thousands separators from the text
      elementalRewardPoints = Integer.parseInt(rewardPointMatcher.group(1).replaceAll(",", ""));
      catalyticRewardPoints = Integer.parseInt(rewardPointMatcher.group(2).replaceAll(",", ""));
    }
    // log.info(msg);
  }

  private void reset() {
    guardians.clear();
    activeGuardians.clear();
    unchargedCellTable = null;
    depositPool = null;
    greatGuardian = null;
    catalyticEssencePile = null;
    elementalEssencePile = null;
    client.clearHintArrow();
    savePoints();
  }

  @Provides
  GuardiansOfTheRiftHelperConfig provideConfig(ConfigManager configManager) {
    return configManager.getConfig(GuardiansOfTheRiftHelperConfig.class);
  }

  /**
   * Quick pass barrier.
   *
   * @param event {@link net.runelite.api.events.MenuOptionClicked}
   */
  @Subscribe
  public void onMenuOptionClicked(MenuOptionClicked event) {
    if (config.quickPassCooldown() == 0) {
      return;
    }

    // Only allow one click on the entry barrier's quick-pass option for every 3 game ticks
    if (event.getId() == 43700 && event.getMenuAction().getId() == 5) {
      if (entryBarrierClickCooldown > 0) {
        event.consume();
      } else {
        entryBarrierClickCooldown = config.quickPassCooldown();
      }
    }
  }

  /**
   * Mutes annoying npc.
   *
   * @param event {@link net.runelite.api.events.OverheadTextChanged}
   */
  @Subscribe
  public void onOverheadTextChanged(OverheadTextChanged event) {
    if (!("Apprentice Tamara".equals(event.getActor().getName())
        || "Apprentice Cordelia".equals(event.getActor().getName()))) {
      return;
    }
    if (config.muteApprentices()) {
      event.getActor().setOverheadText(" ");
    }
  }

  /**
   * Colours portal spawn timer based on spawn window.
   *
   * @param timeSincePortal Seconds since last portal
   * @return {@link Color} to use
   */
  public Color getTimeSincePortalColor(int timeSincePortal) {
    if (isFirstPortal) {
      // first portal takes about 40 more seconds to spawn
      timeSincePortal -= 40;
    }
    if (timeSincePortal >= 108) {
      return Color.RED;
    } else if (timeSincePortal >= 85) {
      return Color.YELLOW;
    }
    return Color.GREEN;
  }

  public int getParentWidgetId() {
    return PARENT_WIDGET_ID;
  }

  public int getPortalWidgetId() {
    return PORTAL_WIDGET_ID;
  }

  public int getPortalSpriteId() {
    return PORTAL_SPRITE_ID;
  }

  /**
   * Renders text.
   *
   * @param g {@link Graphics}
   * @param text text to display
   * @param rect Area
   */
  public void drawCenteredString(Graphics g, String text, Rectangle rect) {
    FontMetrics metrics = g.getFontMetrics();
    int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
    int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
    g.setColor(Color.BLACK);
    g.drawString(text, x + 1, y + 1);
    g.setColor(Color.WHITE);
    g.drawString(text, x, y);
  }

  public int potentialPointsElemental() {
    return potentialPoints(getElementalRewardPoints(), getCurrentElementalRewardPoints());
  }

  public int potentialPointsCatalytic() {
    return potentialPoints(getCatalyticRewardPoints(), getCurrentCatalyticRewardPoints());
  }

  private int potentialPoints(int savedPoints, int currentPoints) {
    if (currentPoints == 0) {
      return savedPoints;
    }
    return savedPoints += currentPoints / 100;
  }

  private void loadPoints() {
    String s = config.bankedPoints();
    if (!s.isEmpty()) {
      String[] ss = s.split(",");
      elementalRewardPoints = Integer.parseInt(ss[0]);
      catalyticRewardPoints = Integer.parseInt(ss[1]);
    }
  }

  private void savePoints() {
    config.bankedPoints(String.format("%d,%d", elementalRewardPoints, catalyticRewardPoints));
  }
}
