class Solution {
    public double findMedianSortedArrays(int[] nums1, int[] nums2) {        

        /* Below two solutions are of time and space complexities of O(m+n)
Solution 1:

int m=nums1.length, n=nums2.length, i=0,j=0;
        int[] num1arr = new int[m+1];
        int[] num2arr = new int[n+1];
        for(int p=0; p<m; p++)
            num1arr[p] = nums1[p];
        for(int p=0; p<n; p++)
            num2arr[p] = nums2[p];
            
        int[] a = new int[m+n];
        num1arr[m]=Integer.MAX_VALUE;
        num2arr[n]=Integer.MAX_VALUE;
        for(int k=0; k<m+n; k++){
            if(num1arr[i]<=num2arr[j]){
                a[k]=num1arr[i];
                i++;
            }
            else{
                a[k]=num2arr[j];
                j++;
            }
        }
        if(((m+n)%2)==1)
            return Double.valueOf(a[((m+n)/2)]);
        else
            return Double.valueOf((a[((m+n-1)/2)]+a[(m+n)/2])/2.0);
       
        
Solution 2:
        int s = nums1.length+nums2.length, p=0, q=0;
        int[] a = new int[s];
        for(int i=0; i<s; i++){
            if(p<nums1.length && q<nums2.length){
                if(nums1[p]<=nums2[q])
                    a[i] = nums1[p++];
                else
                    a[i] = nums2[q++];
            }
            else if(p==nums1.length && q<nums2.length)
                a[i] = nums2[q++];
            else
                a[i] = nums1[p++];
        }
        if(s%2==0)
            return ((a[(s-1)/2]+a[s/2])*1.0)/2;
        else
            return a[s/2]*1.0;*/
        
       

/* Below is the optimized solution of time complexity of O(log(min(x,y)) and space complexity of O(1)

        int small=nums1.length, big=nums2.length;        
        if(small>big)
            return findMedianSortedArrays(nums2,nums1);

        int low=0, high=small, pnums1, pnums2, max_left_nums1, min_right_nums1, max_left_nums2, min_right_nums2;

        while(low<=high){
            pnums1 = (low+high)/2;
            pnums2 = (small+big+1)/2 - pnums1;
            max_left_nums1 = (pnums1==0)?Integer.MIN_VALUE:nums1[pnums1-1];
            min_right_nums1 = (pnums1==small)?Integer.MAX_VALUE:nums1[pnums1];
            
            max_left_nums2 = (pnums2==0)?Integer.MIN_VALUE:nums2[pnums2-1];
            min_right_nums2 = (pnums2==big)?Integer.MAX_VALUE:nums2[pnums2];
            
            if(max_left_nums1<=min_right_nums2 && max_left_nums2<=min_right_nums1){
                if((small+big)%2==0)
                    return (Math.max(max_left_nums1, max_left_nums2)+Math.min(min_right_nums1, min_right_nums2))/2.0;
                else
                    return Math.max(max_left_nums1, max_left_nums2)*1.0;                
            }
            else if(max_left_nums1>min_right_nums2)
                high=pnums1-1;                
            else
                low = pnums1+1;
        }
        return 0.0;
    }
}