package wfmpoc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.*;
import java.util.*;

import com.genesyslab.wfm7.API.util.*;

import com.genesyslab.wfm7.API.service.locator.*;
import com.genesyslab.wfm7.API.service.session700.*;
import com.genesyslab.wfm7.API.service.schedule712.*;
import com.genesyslab.wfm7.API.service.config712.*;

import com.genesyslab.wfm7.API.service.config712.CfgSiteShort;
import com.genesyslab.wfm7.API.service.config712.CfgSiteHolder;
import com.genesyslab.wfm7.API.service.schedule712.CfgAgentHolder;
import com.genesyslab.wfm7.API.service.schedule712.CfgAgentShort;

public class Main extends Application {

    private static String wfmServerHost = "192.168.1.50";
    private static String wfmServerPort = "5007";
    private static String appName = "WFMClient";
    private static String userName = "demo";
    private static String userPassword = "";
    private static int siteId = 1;
    private static int startYear = 2010;
    private static int endYear = 2010;
    private static int startMonth = 8;
    private static int endMonth = 8;
    private static int startDay = 1;
    private static int endDay = 14;
    private static int userDBID = 748;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);

        try {

            int userID = userDBID; // userless or use some user ID


            WFMLocatorServiceSoap locatorService =
                    new WFMLocatorServiceLocator().getWFMLocatorServiceSoap(new URL(
                            "http://" + wfmServerHost + ":" + wfmServerPort + "/?Handler=WFMLocatorService"));

            Location wfmLocation = locatorService.locateServer700("SessionService700", appName, userID, null);

            WFMSessionService700Soap sessionService =
                    new WFMSessionService700Locator().
                            getWFMSessionService700Soap(new URL(wfmLocation.getSessionServiceURL()));

            SessionInfo sessionInfo = sessionService.openSession(appName, userID);

            Hashtable serviceMap = new Hashtable();

            for (int i = 0; i < sessionInfo.getServicesNSizeIs(); ++i) {
                ServiceInfo serviceInfo = sessionInfo.getServices()[i];
                serviceMap.put(serviceInfo.getServiceName(), serviceInfo);

                if (serviceInfo.getServiceName().compareTo("SessionService700") == 0) {
                    sessionService = new WFMSessionService700Locator().
                            getWFMSessionService700Soap(new URL(serviceInfo.getServiceURL()));
                }
            }

            String cfgServiceURL = ((ServiceInfo) serviceMap.get("ConfigService712")).getServiceURL();
            WFMConfigService712Soap cfgService =
                    new WFMConfigService712Locator().getWFMConfigService712Soap(new URL(cfgServiceURL));

            String schServiceURL = ((ServiceInfo) serviceMap.get("ScheduleService712")).getServiceURL();
            WFMScheduleService712Soap schService =
                    new WFMScheduleService712Locator().getWFMScheduleService712Soap(new URL(schServiceURL));

            CfgSiteHolder cfgSiteHolder = cfgService.getSite(
                    null,
                    ECfgSortMode.Site.CFG_SITE_SORT_NAME,
                    true,
                    ECfgInfoType.CFG_INFO_OBJECT_SHORT,
                    false);

            for (int i = 0; i < cfgSiteHolder.getObjectShortArrayNSizeIs(); ++i) {
                CfgSiteShort cfgSite = cfgSiteHolder.getObjectShortArray()[i];

                System.out.println();
                System.out.print(i + 1);
                System.out.print("\tSiteID: ");
                System.out.print(cfgSite.getWmSiteId());
                System.out.print(", Site Name: ");
                System.out.print(cfgSite.getWmName());
                System.out.println();
                System.out.println();

                OleDateTime periodStartDate = new OleDateTime(startYear, startMonth, startDay);
                OleDateTime periodEndDate = new OleDateTime(endYear, endMonth, endDay);


                CfgAgentHolder cfgAgentHolder = schService.getMasterAgent(
                        cfgSite.getWmSiteId(),
                        periodStartDate.asDouble(),
                        periodEndDate.asDouble(),
                        null,
                        null,
                        null,
                        ECfgSortMode.Agent.CFG_AGENT_SORT_CONTRACT_LAST_NAME,
                        true,
                        ECfgInfoType.CFG_INFO_OBJECT_SHORT,
                        false,
                        false,
                        false
                );


                SchAgentStateHolder stateHolder = schService.getAgentState(
                        ESchSource.SCH_SOURCE_COMMITTED,
                        0, // Master Schedule
                        cfgSite.getWmSiteId(),
                        periodStartDate.asDouble(),
                        periodEndDate.addDateTimeSpan(1, 0, 0).asDouble(),
                        null, // teams
                        null, // agents
                        new int[]
                                {
                                        ESchStateType.SCH_STATE_ACTIVITY_SET,
                                        ESchStateType.SCH_STATE_BREAK,
                                        ESchStateType.SCH_STATE_MEAL,
                                        ESchStateType.SCH_STATE_EXCEPTION,
                                        ESchStateType.SCH_STATE_TIMEOFF,
                                        ESchStateType.SCH_STATE_DAYOFF
                                }, // state type filter
                        null, // state ID filter
                        null, // state group filter
                        ESchStateLayout.SCH_LAYOUT_SEQUENTIAL,
                        true);

                Hashtable stateInfoMap = new Hashtable();
                for (int j = 0; j < stateHolder.getSchStateInfoNSizeIs(); ++j) {
                    SchStateInfo stateInfo = stateHolder.getSchStateInfo()[j];
                    long key = (((long) stateInfo.getWmStateType()) << 32) + stateInfo.getWmStateId();
                    stateInfoMap.put(new Long(key), stateInfo);
                }

                for (int j = 0; j < cfgAgentHolder.getObjectShortArrayNSizeIs(); ++j) {
                    CfgAgentShort agentInfo = cfgAgentHolder.getObjectShortArray()[j];

                    System.out.print("\t");
                    System.out.print(j + 1);
                    System.out.print(", Agent Name: "
                            + agentInfo.getGswLastName()
                            + ", "
                            + agentInfo.getGswFirstName());
                    System.out.println();

                    for (int k = 0; k < stateHolder.getSchAgentStateNSizeIs(); ++k) {
                        SchAgentState schAgentState = stateHolder.getSchAgentState()[k];

                        if (schAgentState.getGswAgentId() == agentInfo.getGswAgentId()) {
                            System.out.print("\t\t");

                            switch (schAgentState.getWmStateType()) {
                                case ESchStateType.SCH_STATE_ACTIVITY:
                                    System.out.print("Activity - ");
                                    break;
                                case ESchStateType.SCH_STATE_ACTIVITY_SET:
                                    System.out.print("Activity Set - ");
                                    break;

                                case ESchStateType.SCH_STATE_BREAK:
                                    System.out.print("Break - ");
                                    break;

                                case ESchStateType.SCH_STATE_EXCEPTION:
                                    System.out.print("Exception - ");
                                    break;

                                case ESchStateType.SCH_STATE_MARKED_TIME:
                                    System.out.print("Marked Time - ");
                                    break;

                                case ESchStateType.SCH_STATE_MEAL:
                                    System.out.print("Meal - ");
                                    break;

                                case ESchStateType.SCH_STATE_SHIFT:
                                    System.out.print("Shift - ");
                                    break;

                                case ESchStateType.SCH_STATE_TIMEOFF:
                                    System.out.print("Time Off - ");
                                    break;

                                case ESchStateType.SCH_STATE_DAYOFF:
                                    System.out.print("Day Off - ");
                                    break;

                            }

                            OleDateTime stateStart = new OleDateTime(schAgentState.
                                    getWmStartDateTime());
                            OleDateTime stateEnd = new OleDateTime(schAgentState.
                                    getWmEndDateTime());

                            long key = (((long) schAgentState.getWmStateType()) << 32) +
                                    schAgentState.getWmStateId();

                            SchStateInfo stateInfo =
                                    (SchStateInfo) stateInfoMap.get(new Long(key));


                            if (stateInfo != null)
                                System.out.print(stateInfo.getWmName());

                            if (schAgentState.isWmFullDay())
                                System.out.print(": Full-Day");
                            else
                                System.out.print(": " + stateStart.toDateTimeString() + " - " +
                                        stateEnd.toDateTimeString());

                            if (schAgentState.isWmPaid())
                                System.out.print(", Paid");
                            else
                                System.out.print(", Un-paid");


                            System.out.println();
                        }
                    }
                }
            }

            sessionService.closeSession();
        } catch (org.apache.axis.AxisFault f) {
            String msg = f.getFaultCode().getLocalPart();
            String descr = f.getFaultString();
            System.out.println("Error: " + msg);
            System.out.println("Description: " + descr);
        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println(msg);
        }
    }
}
