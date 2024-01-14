package com.zyq.chirp.common.db.context;

/*
@Slf4j
public class DBContext {
    private static final ThreadLocal<String>dbContext=new ThreadLocal<>();
    private static final AtomicInteger counter=new AtomicInteger(0);
    @Getter
    private static final ArrayList<String>slaveList=new ArrayList<>();
    public static final String MASTER="master";
    private static final String SLAVE="slave";
    public static void addSlave(){
        slaveList.add(SLAVE+slaveList.size()+1);
    }
    public static void set(String db){
        dbContext.set(db);
    }
    public static String get(){
        return dbContext.get();
    }
    public static String getSlave(int index){
        return slaveList.get(index);
    }
    public static void master(){
        set(MASTER);
        log.info("切换到master");
    }
    public static void slave(){
        int index=counter.getAndDecrement()%slaveList.size();
        log.info("slave访问线程数==>{}",counter.get());
        set(slaveList.get(index));
        log.info("切换到slave==>{}",slaveList.get(index));
    }
}
*/
