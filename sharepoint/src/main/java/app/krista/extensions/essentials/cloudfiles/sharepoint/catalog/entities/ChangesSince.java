package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities;

import app.krista.extension.impl.anno.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants.*;

@Domain(id = "catEntryDomain_30c63e77-37f2-4a1d-afd8-f6bfda07ad57",
        name = "Cloud Files",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "7814b7e8-6b0f-4825-bd72-f6a1613be2a2")
@Entity(name = "ChangesSince", id = "localDomainEntity_dbed2233-a2a3-4a43-9619-24ae89e2f460", primaryKey = "Path", options = {})
@Custom
public class ChangesSince {


    @Field.Text(name = "Path", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String path;

    @Field.PickOne(name = UPDATE, values = {"Created", "Updated", "Deleted"}, required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String update;

    @Field.Date(name = DATE, required = true, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long date;

    public static List<ChangesSince> from(List<Map<String, Object>> result) {
        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChangesSince> changesSinceList = new ArrayList<>();
        for (Map<String, Object> map : result) {
            ChangesSince changesSince = new ChangesSince();
            changesSince.path = map.get(PATH) != null ? (String) map.get(PATH) : "";
            changesSince.update = map.get(UPDATE) != null ? (String) map.get(UPDATE) : "";
            changesSince.date = map.get(DATE) instanceof Long ? (Long) map.get(DATE) : null;
            changesSinceList.add(changesSince);
        }
        return changesSinceList;
    }

}