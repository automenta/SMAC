#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>

int main(int argc, char** argv)
{       
  int i=0; 
  for(i=0; i <= argc; i++) 
  {
    printf("%d: %s\n",i,argv[i]);
  }
  
}
