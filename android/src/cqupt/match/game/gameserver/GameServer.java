package cqupt.match.game.gameserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import cqupt.match.game.activity.RoomActivity;


/**
 * 游戏服务器
 * 服务器人数 2 到 6人
 * @author Frontman
 */
public class GameServer extends ServerSocket {
    public static final int PORT = 1999;    /**端口号*/
    public static int number = 1;           /**房间人数,初始只有房主一个人*/
    public boolean gaming = false;          /**是否正在游戏中*/
    public static ServerThread[] client = new ServerThread[6];    /**客户端对象数组*/
    public static int[] map = new int[20];                        /**地图*/

    /**格子属性*/
    public final int JUDGE = 1;             /**问题*/
    public final int BLOOD = 2;             /**加血包*/
    public final int MINE = 3;              /**地雷*/
    public final int PROP = 4;              /**道具框*/

    //增加或减少玩家
    OnAddPlayer addPlayer;

    public GameServer(OnAddPlayer addPlayer) throws IOException {
        super(PORT);
        this.addPlayer = addPlayer;
    }

    /**
     * 服务器开始监听用户连接
     * 连接的客户端保存在客户端数组
     */
    public void listen() {
        while (number < 6 && (!gaming)) {
            try {
                Socket socket = accept();
                if (socket != null) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //初次连接 读取名字
                    out.println(RoomActivity.getuName());
                    String name = in.readLine();
                    addPlayer.addPlayer(name);
                    client[number-1] = new ServerThread(socket, number, new GameControl() {
                        @Override
                        public void beginGame() {
                            begin();
                        }

                        @Override
                        public void endGame() {
                            end();
                        }
                    },addPlayer);
                    if (number==1)
                        client[number-1].start();
                }
            } catch (IOException e) {
                System.out.println("连接错误");
            }
            number++;
        }
    }

    /**
     * 游戏开始，
     * 开启后接收所有客户端的请求
     */
    public void begin() {
        initMap();
        for (ServerThread thread : client) {
            if (thread != null && thread != client[0])
                thread.start();
        }
    }

    /**
     * 游戏结束，
     * 释放占用资源
     */
    public void end() {
        //清空地图
        for (int i : map) {
            i = 0;
        }
        try {
            this.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeClient(int index){
        if (client[index]!=null){
            client[index].close();
        }
    }
    /**
     * 获取所有客户端
     * @return 返回客户端的对象数组
     */
    public static ServerThread[] getClient() {
        return client;
    }

    /**
     * 初始化地图，
     * 随机设定地图格子元素
     */
    public void initMap() {
        //设置道具格
        map[6] = PROP;
        map[10] = PROP;
        map[16] = PROP;

        //随机生成问题格
        map[getRandom()] = JUDGE;
        map[getRandom()] = JUDGE;

        //随机生成地雷
        map[getRandom()] = MINE;
        map[getRandom()] = MINE;

        //随机生成两个加血格
        map[getRandom()] = BLOOD;
        map[getRandom()] = BLOOD;
    }

    /**
     * 获取地图
     * @return 地图元素按照顺序排列的字符串
     */
    public static String getMap() {
        StringBuilder mapStr = new StringBuilder();
        for (int temp : map) {
            mapStr.append(temp);
        }
        return mapStr.toString();
    }

    /**
     * 获取随机值
     * @return 返回一个没有其他属性的格子的坐标，1-19,第一个格子为起始格，无属性
     */
    private int getRandom() {
        Random random = new Random(System.currentTimeMillis());
        int index = random.nextInt(19)+1;
        while (map[index] != 0) {
            index = random.nextInt(19)+1;
        }
        return index;
    }
}
