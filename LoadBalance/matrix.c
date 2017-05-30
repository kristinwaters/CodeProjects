/******************************************************************************
 * Filename: matrix.c
 * Description:  This program multiplies two N by N matrices.  It expects a
 * filename as input.  The file should contain an integer that specifies both
 * the number of rows and columns in the matrices to be multiplied.
 *
 * Author: Kristin Dahl
 * Date: 11-20-2015
 *****************************************************************************/

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>


void matrix(int N)
{
   int i, j, k;

   double A[N][N];
   double B[N][N];
   double C[N][N];

   // Generate random input
   for (i = 0; i < N; i++) {
      for (j = 0; j < N; j++) {
         A[i][j] = rand();
      }
   }

   for (i = 0; i < N; i++) {
      for (j = 0; j < N; j++) {
         B[i][j] = rand();
      }
   }

   for (j = 0; j < N; j++) {
      for (k = 0; k < N; k++) {
         for (i = 0; i < N; i++) {
            C[i][j] += A[i][k] * B[k][j];
         }
      }
   }

   for (k = 0; k < N; k++) {
      for (j = 0; j < N; j++) {
         for (i = 0; i < N; i++) {
            C[i][j] += A[i][k] * B[k][j];
         }
      }
   }

   printf("Done multiplying matrices.\n");
   return;
}


int main(int argc, char *argv[])
{
   // Open file
   FILE * fp = fopen(argv[1], "r");

   if (fp == NULL) {
      printf("Error - file not found.\n");
      return 0;
   }

   // Read file contents
   int N;
   fscanf(fp, "%d", &N);

   printf("N is %d\n", N);

   // Close file
   if (fclose(fp)) {
      printf("Error - unable to close file.\n");
      return 0;
   }

   matrix(N);
   return 1;
}
