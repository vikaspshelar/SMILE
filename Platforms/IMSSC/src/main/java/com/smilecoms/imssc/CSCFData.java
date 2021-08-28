
package com.smilecoms.imssc;

/**
 *
 * @author jaybeepee
 */
class CSCFData {

    int uptime;
    int totalMemory;
    int usedMemory;
    int maxUsedMemory;
    int freeMemory;

    public CSCFData(int uptime, int totalMemory, int usedMemory, int maxUsedMemory, int freeMemory) {
        this.uptime = uptime;
        this.totalMemory = totalMemory;
        this.usedMemory = usedMemory;
        this.maxUsedMemory = maxUsedMemory;
        this.freeMemory = freeMemory;
    }
    
    public CSCFData() {
        
    }
    
    public int getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(int freeMemory) {
        this.freeMemory = freeMemory;
    }

    public int getMaxUsedMemory() {
        return maxUsedMemory;
    }

    public void setMaxUsedMemory(int maxUsedMemory) {
        this.maxUsedMemory = maxUsedMemory;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(int totalMemory) {
        this.totalMemory = totalMemory;
    }

    public int getUptime() {
        return uptime;
    }

    public void setUptime(int uptime) {
        this.uptime = uptime;
    }

    public int getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(int usedMemory) {
        this.usedMemory = usedMemory;
    }
    
}
