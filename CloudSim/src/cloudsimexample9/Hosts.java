package cloudsimexample9;

import org.cloudbus.cloudsim.Host;

import java.util.*;

public final class Hosts implements Iterable<Host> {

    private final List<Host> host_list = new LinkedList<Host>();

    public Hosts(List<? extends Host> hosts) {
        this.host_list.addAll(hosts);
    }

    @Override
    public Iterator<Host> iterator() {
        return get().iterator();
    }

    public boolean add(Host host) {
        return this.host_list.add(host);
    }

    public boolean remove(Host host2Remove) {
        return this.host_list.remove(host2Remove);
    }

    public List<Host> get() {
        return Collections.unmodifiableList(this.host_list);
    }

    public Host getWithMinimumNumberOfPesEquals(int numberOfPes) {
        for (Host host : this.orderedAscByAvailablePes().get())
            if (host.getNumberOfFreePes() >= numberOfPes) return host;
        return null;
    }

    public int size() {
        return this.host_list.size();
    }

    public Hosts orderedAscByAvailablePes() {
        List<Host> list = new ArrayList<Host>(this.host_list);

        list.sort(new Comparator<Host>() {
            @Override
            public int compare(Host o1, Host o2) {
                return Integer.valueOf(o1.getNumberOfFreePes()).compareTo(o2.getNumberOfFreePes());
            }
        });
        return new Hosts(list);
    }
}