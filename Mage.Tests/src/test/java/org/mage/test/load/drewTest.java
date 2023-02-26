package org.mage.test.load;

import mage.remote.Connection;
import org.apache.log4j.Logger;
import mage.remote.Session;
import java.util.*;
import mage.cards.decks.DeckCardLists;
import mage.remote.SessionImpl;
import mage.view.*;
import mage.remote.MageRemoteException;
import org.junit.Assert;
import mage.players.PlayerType;
import org.mage.test.utils.DeckTestUtils;
import mage.constants.*;
import mage.game.match.MatchOptions;


public class drewTest {
    private static final Logger logger = Logger.getLogger(drewTest.class);
    private static final String TEST_USER_NAME = "user";
    private static final String TEST_SERVER = "localhost";
    private static final int TEST_PORT = 17171;
    private static final String TEST_PROXY_TYPE = "None";
    private static final String TEST_AI_GAME_MODE = "Two Player Duel";

    private static final String TEST_AI_DECK_TYPE = "Limited";

    public static void main(String[] args) {
        logger.info("Starting test");
    
        LoadPlayer monitor = new LoadPlayer("m", true);

        GameTypeView gameType = prepareGameType(monitor.session);
        MatchOptions gameOptions = createSimpleGameOptionsForBots(gameType, monitor.session);
        TableView game = monitor.session.createTable(monitor.roomID, gameOptions);
        UUID tableId = game.getTableId();

        DeckCardLists deckListRandom = DeckTestUtils.buildRandomDeckAndInitCards("UB", false, "M20");
        DeckCardLists deckList1 = deckListRandom;
        DeckCardLists deckList2 = deckListRandom;

        Optional<TableView> checkGame;

        monitor.session.joinTable(monitor.roomID, tableId, "ai_1", PlayerType.COMPUTER_MAD, 5, deckList1, "");
        monitor.session.joinTable(monitor.roomID, tableId, "ai_2", PlayerType.COMPUTER_MAD, 5, deckList2, "");
    
        monitor.session.startMatch(monitor.roomID, tableId);
        boolean startToWatching = false;
        while (true){
            GameView gameView = monitor.client.getLastGameView();

            checkGame = monitor.getTable(tableId);
            TableState state = checkGame.get().getTableState();

            logger.warn(checkGame.get().getTableName()
                    + (gameView != null ? ", turn " + gameView.getTurn() + ", " + gameView.getStep().toString() : "")
                    + (gameView != null ? ", active " + gameView.getActivePlayerName() : "")
                    + ", " + state);

            if (state == TableState.FINISHED) {
                /*gameResult.finish(gameView);
                break;*/
                logger.info("Game finished");
            }

            if (!startToWatching && state == TableState.DUELING) {
                Assert.assertTrue(monitor.session.watchGame(checkGame.get().getGames().iterator().next()));
                startToWatching = true;
            }

            if (gameView != null) {
                for (PlayerView p : gameView.getPlayers()) {
                    logger.info(p.getName() + " - Life=" + p.getLife() + "; Lib=" + p.getLibraryCount());
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }


    private static class LoadPlayer {

        String userName;
        Connection connection;
        SimpleMageClient client;
        Session session;
        UUID roomID;
        UUID createdTableID;
        UUID connectedTableID;
        DeckCardLists deckList;
        String lastGameResult = "";

        public LoadPlayer(String userPrefix) {
            this(userPrefix, false);
        }

        public LoadPlayer(String userPrefix, boolean joinGameChat) {
            this.userName = TEST_USER_NAME + "_" + userPrefix;
            this.connection = createSimpleConnection(this.userName);
            this.client = new SimpleMageClient(joinGameChat);
            this.session = new SessionImpl(this.client);

            this.session.connect(this.connection);
            this.client.setSession(this.session);
            this.roomID = this.session.getMainRoomId();
        }

        public ArrayList<UsersView> getAllRoomUsers() {
            ArrayList<UsersView> res = new ArrayList<>();
            try {
                for (RoomUsersView roomUsers : this.session.getRoomUsers(this.roomID)) {
                    res.addAll(roomUsers.getUsersView());
                }
            } catch (MageRemoteException e) {
                logger.error(e);
            }
            return res;
        }

        public UsersView findUser(String userName) {
            for (UsersView user : this.getAllRoomUsers()) {
                if (user.getUserName().equals(userName)) {
                    return user;
                }
            }
            return null;
        }

        public Optional<TableView> getTable(UUID tableID) {
            return this.session.getTable(this.roomID, tableID);
        }

        public UUID createNewTable() {
            
            GameTypeView gameType = prepareGameType(this.session);
            MatchOptions gameOptions = createSimpleGameOptionsForBots(gameType, this.session);
            TableView game = this.session.createTable(this.roomID, gameOptions);
            this.createdTableID = game.getTableId();
            Assert.assertEquals(this.userName, game.getControllerName());

            connectToTable(this.createdTableID);

            return this.createdTableID;
        }

        public void connectToTable(UUID tableID) {
            Assert.assertTrue(this.session.joinTable(this.roomID, tableID, this.userName, PlayerType.HUMAN, 1, this.deckList, ""));
            this.connectedTableID = tableID;
        }

        public void startMatch() {
            Assert.assertNotNull(this.createdTableID);
            Assert.assertTrue(this.session.startMatch(this.roomID, this.createdTableID));
        }

        public void setDeckList(DeckCardLists deckList) {
            this.deckList = deckList;
        }

        public void disconnect() {
            this.session.disconnect(false);
        }

        public void concede() {
            this.client.setConcede(true);
        }
    }

    private static Connection createSimpleConnection(String username) {
        Connection con = new Connection();
        con.setUsername(username);
        con.setHost(TEST_SERVER);
        con.setPort(TEST_PORT);
        Connection.ProxyType proxyType = Connection.ProxyType.valueByText(TEST_PROXY_TYPE);
        con.setProxyType(proxyType);
        return con;
    }
    

    private static GameTypeView prepareGameType(Session session) {
        for (GameTypeView gameType : session.getGameTypes()) {
            logger.info(gameType.getName());
        }
        GameTypeView gameType = session.getGameTypes()
                .stream()
                .filter(m -> m.getName().equals(TEST_AI_GAME_MODE))
                .findFirst()
                .orElse(null);
        Assert.assertNotNull("Can't find game type on the server: " + TEST_AI_GAME_MODE, gameType);
        return gameType;
    }


    private static MatchOptions createSimpleGameOptions(String gameName, GameTypeView gameTypeView, Session session, PlayerType playersType) {
        MatchOptions options = new MatchOptions(gameName, gameTypeView.getName(), true, 2);

        options.getPlayerTypes().add(playersType);
        options.getPlayerTypes().add(playersType);

        for (String deckType : session.getDeckTypes()){
            logger.info(deckType);
        }
        Assert.assertTrue("Can't find game type on the server: " + TEST_AI_DECK_TYPE,
                Arrays.asList(session.getDeckTypes()).contains(TEST_AI_DECK_TYPE));
        
        options.setDeckType(TEST_AI_DECK_TYPE);
        options.setLimited(false);
        options.setAttackOption(MultiplayerAttackOption.MULTIPLE);
        options.setRange(RangeOfInfluence.ALL);
        options.setWinsNeeded(1);
        options.setMatchTimeLimit(MatchTimeLimit.MIN__15);
        return options;
    }

    private static MatchOptions createSimpleGameOptionsForBots(GameTypeView gameTypeView, Session session) {
        return createSimpleGameOptions("Bots test game", gameTypeView, session, PlayerType.HUMAN);
    }
}
