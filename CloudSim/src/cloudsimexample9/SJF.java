package cloudsimexample9;
//SJF

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * An Self test simulation will show how to 20 cloudlets will be distributed among
 * 05 Vms with different MIPS requirements.
 */
public class SJF {

    /** The cloudlet list. */
    private static List<Cloudlet> cloudletList,cloudletListSJF;

    /** The vmlist. */
    private static List<Vm> vmlist;

    private static List<Vm> createVM(int userId, int vms, int idShift) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<Vm> list = new LinkedList<Vm>();

        //VM Parameters
        long size = 1000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 250;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name
        int jk=1;
        //create VMs
        Vm[] vm = new Vm[vms];
        //Log.printLine("*************************************"+ vms);
        for(int i=0;i<vms;i++){

            if (i%2==0 )
                mips += jk;
            else
                mips -= jk;
            vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            jk+=2;
            //else
            //	vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            list.add(vm[i]);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){
        // Creates a container to store Cloudlets
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        //cloudlet parameters
        long length = 4000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for(int i=0;i<cloudlets;i++){
            if (i%2==0 || i<2)
                length +=  6500;
            else
                length -=  3277;

            cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            // setting the owner of these Cloudlets
            cloudlet[i].setUserId(userId);
            list.add(cloudlet[i]);
            Log.printLine("cloudletlist size = " +cloudlet[i].getCloudletTotalLength());
        }

        return list;
    }

    private static void getCloudletListSJF(List<Cloudlet> clist)
    {
        int min=0;
        for (int i=0; i<clist.size();i++)
            if (clist.get(i).getCloudletLength() < clist.get(min).getCloudletLength())
                min=i;
        cloudletListSJF.add(clist.get(min));
        clist.remove(min);
        if (clist.size()!=0)
            getCloudletListSJF(clist);
    }

    @SuppressWarnings("unused")
    private static void sortCloudletList(List<Cloudlet> clist)
    {
        Cloudlet c;
        for (int i=0; i<clist.size()-1; i++)
            if (clist.get(i).getCloudletLength() < clist.get(i+1).getCloudletLength())
            {
                c=clist.get(i);
                clist.add(i, clist.get(i+1));
                clist.add(i+1,c);
                c=null;
            }
    }

    // method to find the average waiting time in a virtual machine
    private static double VmArt(List<Cloudlet> list, int VmId)
    {
        int c = 0;
        double art = 0;
        for(int i=0;i<list.size();i++)
            if (list.get(i).getVmId() == VmId)
            {
                art = art + list.get(i).getExecStartTime();    c++;
            }
        art =  art / c;
        return art;
    }

    private static double VmMakespane(List<Cloudlet> list, int VmId)
    {
        double mkspane = 0;
        for(int i=0;i<list.size();i++)
            if (list.get(i).getVmId() == VmId)
                if (list.get(i).getFinishTime() > mkspane)
                    mkspane =  list.get(i).getFinishTime();
        return mkspane;
    }

    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {
        Log.printLine("Starting CloudSimTestSJF...");

        try {
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            // Second step: Create Datacenters
            //Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            //Third step: Create Broker
            //RoundRobinDatacenterBroker broker = new RoundRobinDatacenterBroker("Broker");

            int vms = 10;
            int cloudlets=50;


            DatacenterBroker broker = createBroker("Broker_0");

            int brokerId = broker.getId();

            //Fourth step: Create five virtual machine
            vmlist  = createVM(brokerId, vms, 0); //creating 1 vms

            //submit vm list to the broker
            broker.submitVmList(vmlist);

            //Fifth step: Create ten Cloudlets
            cloudletList = createCloudlet(brokerId, cloudlets, 0); // creating 10 cloudlets

            Log.printLine("cloudletlist size = " + cloudletList.size());
            cloudletListSJF = new LinkedList<Cloudlet>();
            getCloudletListSJF(cloudletList);
            //sortCloudletList(cloudletList);

            //submit cloudlet list to the broker
            broker.submitCloudletList(cloudletListSJF);


            // Sixth step: Starts the simulation
            CloudSim.startSimulation();


            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList);

            //for (int a=0; a<vmlist.size();a++)
            //	Log.printLine("Average Response Time of Vm-" + vmlist.get(a).getId() + "   =  " + VmArt( newList, vmlist.get(a).getId()));

            //for (int a=0; a<vmlist.size();a++)
            //	Log.printLine("Makespane of Vm-" + vmlist.get(a).getId() + "   =  " + VmMakespane( newList, vmlist.get(a).getId()));

            //Print the debt of each user to each datacenter
            //datacenter0.printDebts();

            Log.printLine("CloudSimTestSJF finished!");
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name){

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store one or more
        //    Machines
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
        //    create a list to store these PEs before creating
        //    a Machine.
        List<Pe> peList1 = new ArrayList<Pe>();

        int mips = 102400;

        // 3. Create PEs and add these into the list.
        //for a quad-core machine, a list of 4 PEs is required:
        peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
        peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(3, new PeProvisionerSimple(mips)));

        //Another list, for a dual-core machine
        List<Pe> peList2 = new ArrayList<Pe>();

        peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

        //4. Create Hosts with its id and list of PEs and add them to the list of machines
        int hostId=0;
        int ram = 102400; //host memory (MB)
        long storage = 1000000; //host storage
        int bw = 200000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList1,
                        new VmSchedulerTimeShared(peList1)
                )
        ); // This is our first machine

        hostId++;

		/*hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList2,
    				new VmSchedulerTimeShared(peList2)
    			)
    		); // Second machine */

        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.1;	// the cost of using storage in this resource
        double costPerBw = 0.1;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }
    
    
    private static DatacenterBroker createBroker(String name){

        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }
    /**
     * Prints the Cloudlet objects
     * @param list  list of Cloudlets
     */
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        double avrt=0;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                      "Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
                Log.print("SUCCESS");
                avrt += cloudlet.getActualCPUTime();
                Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
                               indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                               indent + indent + dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime()));

            }
            else
            {
                Log.print("Failure");
            }


        }
        //Log.printLine("Average execution Time = " + avrt/size);

    }
}

