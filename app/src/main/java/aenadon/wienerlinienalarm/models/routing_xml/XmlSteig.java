package aenadon.wienerlinienalarm.models.routing_xml;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import aenadon.wienerlinienalarm.models.routing_xml.xml_model.MonitorRequest;
import aenadon.wienerlinienalarm.models.routing_xml.xml_model.RoutingXMLRequest;
import aenadon.wienerlinienalarm.models.routing_xml.xml_model.ServingLine;
import aenadon.wienerlinienalarm.models.routing_xml.xml_model.ServingLines;

public class XmlSteig {

    private String line;
    private String direction;
    private String destination;

    public String getLine() {
        return line;
    }

    private void setLine(String line) {
        this.line = line;
    }

    public String getDirection() {
        return direction;
    }

    private void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDestination() {
        return destination;
    }

    private void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public String toString() {
        return line + " " + destination;
    }

    public static List<XmlSteig> getStationLines(RoutingXMLRequest xmlRequest) {
        MonitorRequest monitorRequest = xmlRequest.getMonitorRequest();
        ServingLines xmlServingLines = monitorRequest.getServingLines();
        List<XmlSteig> flattenedSteigList = new ArrayList<>();
        if (xmlServingLines != null) {
            List<ServingLine> servingLineList = monitorRequest.getServingLines().getLines();
            if (servingLineList != null) {
                Stream.of(servingLineList).map(XmlSteig::mapLineToSteig).forEach(flattenedSteigList::add);
            }
        }
        return flattenedSteigList;
    }

    private static XmlSteig mapLineToSteig(ServingLine servingLine) {
        XmlSteig flattenedSteig = new XmlSteig();
        flattenedSteig.setLine(servingLine.getLine());
        flattenedSteig.setDirection(servingLine.getDirectionParam().getDirection());
        flattenedSteig.setDestination(servingLine.getDestination().replace("Wien ", ""));
        return flattenedSteig;
    }
}
