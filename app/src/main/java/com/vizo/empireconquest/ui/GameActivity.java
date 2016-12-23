package com.vizo.empireconquest.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.vizo.empireconquest.R;
import com.vizo.empireconquest.models.Board;
import com.vizo.empireconquest.models.Player;
import com.vizo.empireconquest.models.Territory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener, RoomUpdateListener, Serializable{

    final static String TAG = "Ben Vissotzky";

    // Request codes for the UIs that we show with startActivityForResult:
    final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    String mRoomId = null;

    // The participants in the currently active game
    ArrayList<Participant> mParticipants = null;

    // My participant ID in the currently active game
    String mMyId = null;

    // Message buffer for sending messages
    byte[] mMsgBuf = new byte[2];

    // Board object
    Board board;

    // Player Array
    ArrayList<Player> players = new ArrayList<>();
    Player me;

    //Keeping track of player inputs, determining next round or not


    //Territory Array
    ArrayList<Territory> territories = new ArrayList<>();


    //For Battles
    boolean territorySelected = false;
    Territory firstTerritory;
    Territory secondTerritory;

    //Game State
    boolean gameOn = false;
    boolean attack = false;
    boolean reinforce = false;
    long lastReinforce = System.currentTimeMillis();
    boolean unSynced = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_map);
        switchToScreen(R.id.screen_wait);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        mGoogleApiClient.connect();

        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        update();
        if (!territorySelected) {
            territorySelected = true;
            for (int i = 0; i < CLICKABLES.length; i++) {
                if (v.getId() == CLICKABLES[i]) {
                    firstTerritory = findTerritory(i);
                }
            }
        } else {
            for (int i = 0; i < CLICKABLES.length; i++) {
                if (v.getId() == CLICKABLES[i]) {
                    secondTerritory = findTerritory(i);
                }
            }
        }
        if ((firstTerritory != null) && (secondTerritory != null)) {
            territorySelected = false;
            Territory.territoryBattle(firstTerritory, secondTerritory);
            for (Participant p : mParticipants) {
                if (p.getParticipantId() != mMyId) {
                    byte[] bytes = new byte[3];
                    bytes[0] = 2;
                    bytes[1] = (byte) firstTerritory.getIndex();
                    bytes[2] = (byte) secondTerritory.getIndex();

                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, bytes, mRoomId, p.getParticipantId());
                }
            }
            firstTerritory = null;
            secondTerritory = null;
            updateTerritoryUi();


        }
    }

    void startQuickGame() {
        //quick game with 1 random opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 2;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS, MAX_OPPONENTS, 0);
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        keepScreenOn();
        resetGameVars();
        Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode,
                                 Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "Starting game (waiting room returned OK).");
                    startGame();
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }


    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.w(TAG,
                    "GameHelper: client was already connected on onStart()");
        } else {
            Log.d(TAG, "Connecting client.");
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
//            switchToScreen(R.id.screen_sign_in);
        } else {
            switchToScreen(R.id.screen_wait);
        }
        super.onStop();
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
            leaveRoom();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        stopKeepingScreenOn();
        if (mRoomId != null) {
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
            mRoomId = null;
            switchToScreen(R.id.screen_wait);
        } else {
//            switchToMainScreen();
        }
    }

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected() called. Sign in successful!");

        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
//        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        if (connectionHint != null) {
            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
            Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG, "onConnected: connection hint has a room invite!");
//                acceptInviteToRoom(inv.getInvitationId());
                return;
            }
        }
        startQuickGame();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, "Ruh Roh");
        }

//        switchToScreen(R.id.screen_sign_in);
    }

    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        //get participants and my ID:
        mParticipants = room.getParticipants();
        int playerNumber = 1;
        for (Participant p : mParticipants) {
            Player newPlayer = new Player(p.getDisplayName(), p.getParticipantId(), playerNumber);
            playerNumber++;
            players.add(newPlayer);
            if (p.getParticipantId() == mMyId) {
                me = newPlayer;
            }
        }
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

        // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
        if (mRoomId == null)
            mRoomId = room.getRoomId();

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + mRoomId);
        Log.d(TAG, "My ID " + mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }


    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        mRoomId = null;
        showGameError();
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // save room ID so we can leave cleanly before the game starts.
        mRoomId = room.getRoomId();

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    void showGameError() {
        BaseGameUtils.makeSimpleDialog(this, "Game problem?");
        switchToMainScreen();
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }


    void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
        if (mParticipants != null) {

        }
    }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */

    // desired fps
    private final static int    MAX_FPS = 50;
    // maximum number of frames to be skipped
    private final static int    MAX_FRAME_SKIPS = 5;
    // the frame period
    private final static int    FRAME_PERIOD = 1000 / MAX_FPS;


//    @Override
//    public void run() {
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
//        Canvas canvas;
//        Log.d(TAG, "Starting game loop");
//        long beginTime;     // the time when the cycle begun
//
//        long timeDiff;      // the time it took for the cycle to execute
//
//        int sleepTime;      // ms to sleep (<0 if we're behind)
//
//        int framesSkipped;  // number of frames being skipped
//        sleepTime = 0;
//        while (gameOn) {
//            try {
//                beginTime = System.currentTimeMillis();
//                framesSkipped = 0;  // resetting the frames skipped
//                // update game state
//                update();
//                // render state to the screen
//                // draws the canvas on the panel
//                updateTerritoryUi();
//                Log.d(TAG, "here");
//                // calculate how long did the cycle take
//                timeDiff = System.currentTimeMillis() - beginTime;
//                // calculate sleep time
//                sleepTime = (int)(FRAME_PERIOD - timeDiff);
//                if (sleepTime > 0) {
//                    // if sleepTime > 0 we're OK
//                    try {
//                        // send the thread to sleep for a short period
//                        // very useful for battery saving
//                        Thread.sleep(sleepTime);
//                    } catch (InterruptedException e) {}
//                while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
//                    // we need to catch up
//                    // update without rendering
//                    update();
//                    // add frame period to check if in next frame
//                    sleepTime += FRAME_PERIOD;
//                    framesSkipped++;
//                }
//                }
//            } finally {
//                // in case of an exception the surface is not left in
//                // an inconsistent state
//
//            }   // end finally
//        }
//    }
//    @Override
//    public void run()
//    {
//        //runs once at beginning of game until territories are synced
//        updateTerritoryUi();
////        Looping until the boolean is false
//        while (gameOn)
//        {
//            if (mCurScreen != R.id.screen_game) switchToScreen(R.id.screen_game);
////            processInput();
//            update();
//        }
//    }

    // Reset game variables in preparation for a new game.
    void resetGameVars() {
        // Deletes room and stuff
    }

    // determines creator of board, initializes board sync and starts main game loop
    void startGame() {
        switchToScreen(R.id.screen_game);
        board = new Board();
        players = sortPlayers(players);
        if (players.get(0).getPlayerId() == mMyId) {
            createBoard(players);
            syncTerritories(board.getTerritories(), players);
            updateTerritoryUi();
        }
        gameOn = true;
    }

    //Sorts Players by id and determines creator of board
    public ArrayList<Player> sortPlayers(ArrayList<Player> players) {
        Collections.sort(players, new Comparator<Player>() {
            public int compare(Player p1, Player p2) {
                return p1.getName().compareTo(p2.getName());
            }
        });
        return players;
    }

    //Only called by creator of board. Determines random owners of territories
    public void createBoard(ArrayList<Player> players) {
        board.assignTerritories(players);
        territories = board.getTerritories();
    }

    public void update() {
        if (System.currentTimeMillis() - lastReinforce > 5000) {
            Log.d(TAG, "reifnorce time");
            lastReinforce = System.currentTimeMillis();
            reinforce = true;
        }
        if (reinforce) {
            reinforceAll();
            updateTerritoryUi();
        }
    }

    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

    // Called when we receive a real-time message from the network.
    // Message of 0 means board generation command received
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        switch (buf[0]) {
            case 0:
                syncGameData(buf, players);
                break;
            case 2:
                Territory.territoryBattle(findTerritory((int) buf[1]), findTerritory((int) buf[2]));
                updateTerritoryUi();
                break;
        }
//        Toast.makeText(GameActivity.this, "Message received: " + (int) buf[0] + "/" + (int) buf[1], Toast.LENGTH_SHORT).show();
    }

    public void syncGameData(byte[] buf, ArrayList<Player> players) {
        ArrayList<String> indexedTerritories = board.getIndexedTerritories();
        int t = 1;
        int p = 2;
        //TODO dont hardcode you noob
        for (int i = 0; i < 42; i++) {
            Territory newTerritory = new Territory(indexedTerritories.get(buf[t]), players.get(buf[p]), buf[t]);
            territories.add(newTerritory);
            players.get(buf[p]).newTerritory(newTerritory);
            t += 2;
            p += 2;
        }
    }

    //converts territory array into byte array to transmit over GPS API
    public void syncTerritories(ArrayList<Territory> territories, ArrayList<Player> players) {
        byte[] array = new byte[territories.size() * 2 + 1];
        //sets command of byte array to instruct message receiver
        array[0] = (byte) 0;
        ArrayList<String> indexedTerritories = board.getIndexedTerritories();
        //sets index to assign territory and player
        int tI = 1;
        int pI = 2;
        for (int i = 0; i < territories.size(); i++) {
            //associates territory object with its territory name in indexedTerritories
            int territory = indexedTerritories.indexOf(territories.get(i).getName());

            //associates player with index of player in shared players array
            int player = players.indexOf(territories.get(i).getPlayerOwned());

            //saves territory index and player index to byte[]
            array[tI] = (byte) territory;
            array[pI] = (byte) player;

            tI += 2;
            pI += 2;
        }
        for (Participant p : mParticipants) {
            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, array, mRoomId, p.getParticipantId());
        }
    }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // update Territory troops
    public void reinforceAll() {
        reinforce = false;
        for (Player p : players) {
            Log.d(TAG, "loopin");
            for (Territory t : p.getTerritories()) {
                t.addReinforcements();
                Log.d(TAG, Integer.toString(t.getTroops()));
            }
        }
    }

    //Will color buttons and update text of all territories
    public void updateTerritoryUi() {
        if (territories.size() > 10) {
            final int[] TestTROOPS = TROOPS;
//            new Thread() {
//                public void run() {
                    try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (Player p : players) {
                                for (Territory t: p.getTerritories()) {
                                    TextView textView = (TextView) findViewById(TestTROOPS[t.getIndex()]);
                                    textView.setText(null);
                                    textView.append(Integer.toString(t.getTroops()));
                                    if (t.getPlayerOwned().getPlayerNumber().equals("player1")) {
                                        textView.setTextColor(Color.RED);
                                    }

                                    if (t.getPlayerOwned().getPlayerNumber().equals("player2")) {
                                        textView.setTextColor(Color.BLUE);
                                    }if (t.getPlayerOwned().getPlayerNumber().equals("player3")) {
                                        textView.setTextColor(Color.GREEN);
                                    }
                                }
                            }
                        }
                    });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                }
//            }.start();
//
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    updateTerritoryUi();
                }
            }, 2000);
        }
    }


    final static int[] CLICKABLES = {
            R.id.button_alaska, R.id.button_nwTerritory, R.id.button_greenland, R.id.button_alberta, R.id.button_ontario, R.id.button_quebec, R.id.button_westernUs, R.id.button_easternUs, R.id.button_centralAmerica, R.id.button_venezuela, R.id.button_brazil, R.id.button_peru, R.id.button_argentina, R.id.button_southAfrica, R.id.button_madagascar, R.id.button_congo, R.id.button_eastAfrica, R.id.button_egypt, R.id.button_northAfrica, R.id.button_westernEurope,R.id.button_southernEurope, R.id.button_northernEurope, R.id.button_greatBritain, R.id.button_ukraine, R.id.button_scandinavia, R.id.button_iceland, R.id.button_middleEast, R.id.button_afghanistan, R.id.button_ural,R.id.button_siberia, R.id.button_india, R.id.button_china, R.id.button_mongolia, R.id.button_irkutsk, R.id.button_yakutsk, R.id.button_kamchatka, R.id.button_japan, R.id.button_siam, R.id.button_indonesia,R.id.button_newGuinea, R.id.button_westernAustralia, R.id.button_easternAustralia
    };
    final static int[] TROOPS = {
            R.id.troops_alaska, R.id.troops_nwTerritory, R.id.troops_greenland, R.id.troops_alberta, R.id.troops_ontario, R.id.troops_quebec, R.id.troops_westernUs, R.id.troops_easternUs, R.id.troops_centralAmerica, R.id.troops_venezuela, R.id.troops_brazil, R.id.troops_peru, R.id.troops_argentina, R.id.troops_southAfrica, R.id.troops_madagascar, R.id.troops_congo, R.id.troops_eastAfrica, R.id.troops_egypt, R.id.troops_northAfrica, R.id.troops_westernEurope,R.id.troops_southernEurope, R.id.troops_northernEurope, R.id.troops_greatBritain, R.id.troops_ukraine, R.id.troops_scandinavia, R.id.troops_iceland, R.id.troops_middleEast, R.id.troops_afghanistan, R.id.troops_ural,R.id.troops_siberia, R.id.troops_india, R.id.troops_china, R.id.troops_mongolia, R.id.troops_irkutsk, R.id.troops_yakutsk, R.id.troops_kamchatka, R.id.troops_japan, R.id.troops_siam, R.id.troops_indonesia,R.id.troops_newGuinea, R.id.troops_westernAustralia, R.id.troops_easternAustralia
    };

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_game,
            R.id.screen_wait
    };
    int mCurScreen = -1;

    void switchToScreen(int screenId) {
        //make the requested screen visible; hide all others
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;
    }

    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    void switchToMainScreen() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
//            switchToScreen(R.id.screen_main);
            Log.d(TAG, "switchToMainScreen if");
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            Log.d(TAG, "switchToMainScreen else");
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
//            switchToScreen(R.id.screen_sign_in);
        }
    }

    public Territory findTerritory(int index) {
        Territory territory;
        for (Territory t : territories) {
            if (t.getIndex() == index) {
                territory = t;
                return territory;
            }
        }
        return null;
    }
}
