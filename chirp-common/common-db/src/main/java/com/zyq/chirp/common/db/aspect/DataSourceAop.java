package com.zyq.chirp.common.db.aspect;

/*
import com.zyq.chirp.common.db.context.DBContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DataSourceAop {
    @Pointcut("(within(@com.zyq.chirp.common.db.aspect.MultiDataSource *)" +
            " && (execution(* add*(..))" +
            "||execution(* update*(..))" +
            "||execution(* modify*(..))" +
            "||execution(* insert*(..))" +
            "||execution(* edit*(..))" +
            "||execution(* del*(..))" +
            "||execution(* remove*(..))))||@annotation(com.zyq.chirp.common.db.aspect.MultiDataSource)")
    public void writePointcut(){};
    @Pointcut("(within(@com.zyq.chirp.common.db.aspect.MultiDataSource *)" +
            " && (execution(* get*(..))" +
            "||execution(* select*(..))))||@annotation(com.zyq.chirp.common.db.aspect.MultiDataSource)")
    public void readPointcut(){};
    @Before("writePointcut()")
    public void write(){
        DBContext.master();
    }
    @Before("readPointcut()")
    public void read(){
        DBContext.slave();
    }
}
*/
