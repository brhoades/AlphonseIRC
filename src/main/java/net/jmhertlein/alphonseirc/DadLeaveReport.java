/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jmhertlein.alphonseirc;

import java.time.ZonedDateTime;

/**
 *
 * @author joshua
 */
public class DadLeaveReport {
    private final ZonedDateTime time;
    private final String reporter;

    public DadLeaveReport(String time, String reporter) {
        this.time = ZonedDateTime.parse(time);
        this.reporter = reporter;
    }

    public DadLeaveReport(String reporter) {
        this.time = ZonedDateTime.now();
        this.reporter = reporter;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public String getReporter() {
        return reporter;
    }
}
