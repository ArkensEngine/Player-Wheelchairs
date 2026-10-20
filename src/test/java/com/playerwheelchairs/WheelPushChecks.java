package com.playerwheelchairs;


final class WheelPushChecks
{
    static void run()
    {
        for(int i=0;i<70;i++)
        {
            double distance=i*.5;
            WheelPushCycle.Hand left=WheelPushCycle.hand(-1,distance),right=WheelPushCycle.hand(1,distance);
            require(left.x==-right.x && left.y==right.y && left.z==right.z,"Mirrored hand targets share a stroke");
            require(Math.abs(Math.hypot(left.y-ChairMesh.REAR_AXLE_Y,left.z-ChairMesh.REAR_AXLE_Z)-ChairMesh.REAR_RADIUS)<1e-7,"Contact hand lies on rear-wheel rim");
            double theta=Math.atan2(left.y-ChairMesh.REAR_AXLE_Y,left.z-ChairMesh.REAR_AXLE_Z);
            double start=-Math.PI/2+WheelPushCycle.TRAVEL_PER_CYCLE*WheelPushCycle.CONTACT_FRACTION/(2*ChairMesh.REAR_RADIUS);
            require(Math.abs(theta-(start-distance/ChairMesh.REAR_RADIUS))<1e-7,"Contact stroke follows actual wheel angular travel");
        }
        for(double join:new double[]{0,WheelPushCycle.CONTACT_FRACTION*WheelPushCycle.TRAVEL_PER_CYCLE,WheelPushCycle.TRAVEL_PER_CYCLE})
        {
            double h=.0001;
            WheelPushCycle.Hand before=WheelPushCycle.hand(1,join-h),at=WheelPushCycle.hand(1,join),after=WheelPushCycle.hand(1,join+h);
            require(Math.abs((at.y-before.y)/h-(after.y-at.y)/h)<.001 && Math.abs((at.z-before.z)/h-(after.z-at.z)/h)<.001,"Contact/recovery joins have continuous position and velocity");
        }
        require(WheelPushCycle.mayPush(true,-1,true),"Moving free hands may push");
        require(!WheelPushCycle.mayPush(true,424,true)&&!WheelPushCycle.mayPush(true,422,true),"Hit reactions and attacks take precedence");
        require(!WheelPushCycle.mayPush(false,-1,true)&&!WheelPushCycle.mayPush(true,-1,false),"Idle and occupied hands do not push");
        System.out.println("PASS: authoring hand/rim contact, wheel synchronisation, smooth recovery joins and action priority.");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
