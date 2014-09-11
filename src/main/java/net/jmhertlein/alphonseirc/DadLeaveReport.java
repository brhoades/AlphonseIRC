/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jmhertlein.alphonseirc;

import java.time.LocalTime;

/**
 *
 * @author joshua
 */
public class DadLeaveReport implements Comparable<DadLeaveReport> {
    private final LocalTime time;
    private final String reporter;

    public DadLeaveReport(String time, String reporter) {
        this.time = LocalTime.parse(time);
        this.reporter = reporter;
    }

    public DadLeaveReport(String reporter) {
        this.time = LocalTime.now();
        this.reporter = reporter;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getReporter() {
        return reporter;
    }

    @Override
    public int compareTo(DadLeaveReport o) {
        return getTime().compareTo(o.getTime());
    }
}
