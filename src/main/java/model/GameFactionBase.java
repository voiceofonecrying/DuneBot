package model;

import java.util.ArrayList;
import java.util.List;

public class GameFactionBase {
    private final List<Resource> resources;

    public GameFactionBase() {
        this.resources = new ArrayList<>();
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void addResource(Resource resource) {
        resources.add(resource);
    }

    public void removeResource(String resourceName) {
        this.resources.remove(getResource(resourceName));
    }


    public Resource getResource(String name) {
        return resources.stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .get();
    }
}
