package com.shixin.test;
import com.shixin.serviceimpl.BaseUrlsManagerServiceImpl;
import java.util.List;
public class test_1 {
    public static void main(String[] args) {
        BaseUrlsManagerServiceImpl base=new BaseUrlsManagerServiceImpl();
        List list=base.loadPotional_Urls();
        System.out.print(list);
    }
}
