package com.shixin.serviceimpl;
import java.text.DecimalFormat;

import org.springframework.stereotype.Service;

import com.shixin.service.ServerStatusService;

@Service
public class ServerStatusServiceImpl implements ServerStatusService {

   private static final DecimalFormat DF = new DecimalFormat("#.00");

    // 实现接口中的 getServerStatus 方法，写具体逻辑
    @Override
    public String getStatus() {
        // collect basic JVM/server metrics using Runtime
        Runtime rt = Runtime.getRuntime();
        int processors = rt.availableProcessors();
        long freeMem = rt.freeMemory();
        long totalMem = rt.totalMemory();
        long maxMem = rt.maxMemory();

        // memory usage rate
        double usedMem = totalMem - freeMem;
        double memUsageRate = usedMem / maxMem * 100.0;

        // cpu load from OS
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
                        .getOperatingSystemMXBean();
        double cpuLoad = osBean.getCpuLoad() * 100.0; // 0.0-100.0

        StringBuilder sb = new StringBuilder();
        sb.append("CPU 核心数: ").append(processors).append(" 核, ");
        sb.append("CPU 使用率: ").append(DF.format(cpuLoad)).append("%, ");
        sb.append("内存总量: ").append(DF.format(maxMem / 1024.0 / 1024)).append("MB, ");
        sb.append("已用内存: ").append(DF.format(usedMem / 1024.0 / 1024)).append("MB, ");
        sb.append("内存使用率: ").append(DF.format(memUsageRate)).append("%");

        return sb.toString();
    }
}
