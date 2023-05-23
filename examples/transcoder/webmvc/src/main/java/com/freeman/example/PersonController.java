package com.freeman.example;

import com.freemanan.sample.pet.v1.GetPetRequest;
import com.freemanan.sample.pet.v1.Pet;
import com.freemanan.sample.pet.v1.PetServiceGrpc;
import com.freemanan.starter.grpc.server.GrpcService;
import io.grpc.stub.StreamObserver;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author Freeman
 */
@GrpcService
public class PersonController extends PetServiceGrpc.PetServiceImplBase {

    @Override
    @PostMapping("/grpcstarter.testing.v1.PersonService/GetPerson/**")
    public void getPet(GetPetRequest request, StreamObserver<Pet> responseObserver) {
        Pet pet = Pet.newBuilder()
                .setName(request.getName())
                .setAge(18)
                .addFavoriteFoods("banana")
                .build();
        if (request.getName().startsWith("err")) {
            throw new IllegalArgumentException("error");
        }
        responseObserver.onNext(pet);
        responseObserver.onCompleted();
    }
}
